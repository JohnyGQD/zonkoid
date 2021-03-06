package eu.urbancoders.zonkysniper.integration;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import eu.urbancoders.zonkysniper.core.Constants;
import eu.urbancoders.zonkysniper.core.ZonkySniperApplication;
import eu.urbancoders.zonkysniper.dataobjects.ConfigurationItem;
import eu.urbancoders.zonkysniper.dataobjects.Investment;
import eu.urbancoders.zonkysniper.dataobjects.Investor;
import eu.urbancoders.zonkysniper.dataobjects.WalletTransaction;
import eu.urbancoders.zonkysniper.dataobjects.ZonkoidWallet;
import eu.urbancoders.zonkysniper.events.BookPurchase;
import eu.urbancoders.zonkysniper.events.GetConfiguration;
import eu.urbancoders.zonkysniper.events.GetZonkoidWallet;
import eu.urbancoders.zonkysniper.events.LoginCheck;
import eu.urbancoders.zonkysniper.events.Bugreport;
import eu.urbancoders.zonkysniper.events.FcmTokenRegistration;
import eu.urbancoders.zonkysniper.events.GetInvestmentsByZonkoid;
import eu.urbancoders.zonkysniper.events.LogInvestment;
import eu.urbancoders.zonkysniper.events.RegisterThirdpartyNotif;
import eu.urbancoders.zonkysniper.events.UnregisterThirdpartyNotif;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.solovyev.android.checkout.Purchase;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Author: Ondrej Steger (ondrej@steger.cz)
 * Date: 04.07.2016
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
public class UrbancodersClient {

    private static final String TAG = UrbancodersClient.class.getName();
    private static final String BASE_URL = "https://urbancoders.eu/";
//    private static final String BASE_URL = "http://10.0.2.2:8080/";  // TOxDO remove fejk URL

    private static Retrofit retrofit;
    private UrbancodersService ucService;

    public UrbancodersClient() {

        EventBus.getDefault().register(this);

        ZonkoidLoggingInterceptor interceptor = new ZonkoidLoggingInterceptor();
        interceptor.setLevel(ZonkoidLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .create();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .build();

        ucService = retrofit.create(UrbancodersService.class);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void loginCheck(LoginCheck.Request evt) {
        if(ZonkySniperApplication.getInstance().getUser() == null) {
            Log.e(TAG, "Uzivatel neni prihlaseny, nelze zavolat checkpoint");
            return;
        }

        Call<Investor> call = ucService.loginCheck(evt.getInvestor());

        try {
            Response<Investor> response = call.execute();
            if(response.isSuccessful() && response.body() != null) {
                ZonkySniperApplication.getInstance().setZonkyCommanderStatus(response.body().getZonkyCommanderStatus());
            } else {
                // nechci klienta mucit, povolim mu vsechno :)
                Log.e(TAG, "Nepodarilo se ziskat stav investora na checkpointu.");
                ZonkySniperApplication.getInstance().setZonkyCommanderStatus(Investor.Status.ACTIVE);
            }

        } catch (IOException e) {
            Log.e(TAG, "Failed to check user on login.", e);
            // nechci klienta mucit, povolim mu vsechno :)
            ZonkySniperApplication.getInstance().setZonkyCommanderStatus(Investor.Status.ACTIVE);
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void sendBugreport(Bugreport.Request evt) {
        Call<String> call = ucService.sendBugreport(
                evt.getUsername(),
                evt.getDescription(),
                evt.getLogs(),
                evt.getTimestamp(),
                Constants.ClientApps.ZONKOID
        );

        try {
            Response<String> response = call.execute();
            if (response != null && response.isSuccessful()) {
                EventBus.getDefault().post(new Bugreport.Response());
            }
        } catch (IOException e) {
            Log.e(TAG, "Nezdarilo se ulozeni bugreportu. "+e);
        }
    }

    /**
     * Zaloguje investici, asynchronne tak, aby neobtezoval thready
     * @param evt udalost
     */
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void logInvestment(LogInvestment.Request evt) {
        Call<ResponseBody> call = ucService.logInvestment(
                evt.getUsername(),
                Constants.ClientApps.ZONKOID,
                evt.getMyInvestment()
        );

        try {
            Response<ResponseBody> response = call.execute();
            if (response != null && response.isSuccessful()) {
                ZonkySniperApplication.getInstance().setZonkyCommanderStatus(Investor.Status.valueOf(response.body().string()));
            } else {
                ZonkySniperApplication.getInstance().setZonkyCommanderStatus(Investor.Status.ACTIVE);
            }
        } catch (IOException e) {
            Log.w(TAG, "Failed to log investment to zonkycommander. "+e.getMessage());
        }
    }

    /**
     * Ziskat seznam investic pres Zonkoida do dane pujcky
     * @param evt
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void getInvestmentsByZonkoid(GetInvestmentsByZonkoid.Request evt) {
        Call<List<Investment>> call = ucService.getInvestmentsByZonkoid(
                evt.getLoanId()
        );

        try {
            Response<List<Investment>> response = call.execute();
            if(response != null && response.isSuccessful()) {
                EventBus.getDefault().post(new GetInvestmentsByZonkoid.Response(response.body()));
            }
        } catch (IOException e) {
            Log.w(TAG, "Failed to get investments by Zonkoid. "+e.getMessage());
        }

    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void registerUserAndThirdParty(RegisterThirdpartyNotif.Request evt) {
        Call<String> call = ucService.registerUserAndThirdParty(evt.getUsername(), evt.getClientApp().name());

        try {
            Response<String> response = call.execute();
            if (response != null && response.isSuccessful()) {
                EventBus.getDefault().post(new RegisterThirdpartyNotif.Response(Integer.parseInt(response.body())));
            } else {
                EventBus.getDefault().post(new RegisterThirdpartyNotif.Failure());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to registerUserAndThirdParty and get code", e);
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void unregisterUserAndThirdParty(UnregisterThirdpartyNotif.Request evt) {
        Call<String> call = ucService.unregisterUserAndThirdParty(evt.getUsername(), evt.getClientApp().name());

        try {
            Response<String> response = call.execute();
            if (response != null && response.isSuccessful()) {
                EventBus.getDefault().post(new UnregisterThirdpartyNotif.Response());
            } else {
                EventBus.getDefault().post(new RegisterThirdpartyNotif.Failure());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to unregisterUserAndThirdParty and get code", e);
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void registerUserToFcm(FcmTokenRegistration.Request evt) {
        Call<Void> call = ucService.registerUserToFcm(evt.getUsername(), evt.getToken());

        try {
            Response<Void> response = call.execute();
            if (response != null && response.isSuccessful()) {
//                EventBus.getDefault().post(new FcmTokenRegistration.Response());
            } else {
//                EventBus.getDefault().post(new FcmTokenRegistration.Failure());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to registerUserToFcm.", e);
        }
    }

    /**
     * Vraci data pro Zonkoid Wallet
     * @param evt
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void getZonkoidWallet(GetZonkoidWallet.Request evt) {
        Call<ZonkoidWallet> call = ucService.getZonkoidWallet(
                evt.getInvestorId()
        );

        try {
            Response<ZonkoidWallet> response = call.execute();
            if (response != null && response.isSuccessful()) {
                EventBus.getDefault().post(new GetZonkoidWallet.Response(response.body()));
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to get Zonkoid wallet. " + e.getMessage());
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void bookPurchase(BookPurchase.Request evt) {

        Purchase tmpPurchase = evt.getPurchase();
        WalletTransaction purchaseForZC = new WalletTransaction();
        purchaseForZC.setAmount(evt.getPriceToPay());
        purchaseForZC.setTransactionDate(new Date(tmpPurchase.time));
        purchaseForZC.setPurchaseToken(tmpPurchase.token);
        purchaseForZC.setPurchaseSKU(tmpPurchase.sku);
        purchaseForZC.setOrderId(tmpPurchase.orderId);

        Call<String> call = ucService.bookPurchase(
                evt.getInvestorId(), Constants.ClientApps.ZONKOID, purchaseForZC
        );

        try {
            Response<String> response = call.execute();
            if (response != null && response.isSuccessful()) {
                EventBus.getDefault().post(new BookPurchase.Response(Boolean.valueOf(response.body()), tmpPurchase));
            } else {
                Log.e(TAG, "Nepodarilo se zavolat ZC pro booking purchasu");
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to book purchase. " + e.getMessage());
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void getConfiguration(GetConfiguration.Request evt) {
        Call<List<ConfigurationItem>> call = ucService.getConfiguration(
                evt.getKeys()
        );

        try {
            Response<List<ConfigurationItem>> response = call.execute();
            if (response != null && response.isSuccessful()) {
                EventBus.getDefault().post(new GetConfiguration.Response(response.body()));
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to get configuration items from database. " + e.getMessage());
        }
    }

}
