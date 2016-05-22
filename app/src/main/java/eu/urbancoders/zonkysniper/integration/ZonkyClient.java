package eu.urbancoders.zonkysniper.integration;

import eu.urbancoders.zonkysniper.ZonkySniperApplication;
import eu.urbancoders.zonkysniper.dataobjects.AuthToken;
import eu.urbancoders.zonkysniper.dataobjects.Loan;
import eu.urbancoders.zonkysniper.dataobjects.Wallet;
import eu.urbancoders.zonkysniper.dataobjects.ZonkyAPIError;
import eu.urbancoders.zonkysniper.events.GetWallet;
import eu.urbancoders.zonkysniper.events.Invest;
import eu.urbancoders.zonkysniper.events.UnresolvableError;
import eu.urbancoders.zonkysniper.events.UserLogin;
import eu.urbancoders.zonkysniper.events.ReloadMarket;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Author: Ondrej Steger (ondrej@steger.cz)
 * Date: 14.05.2016
 */
public class ZonkyClient {
    private static final String TAG = ZonkyClient.class.getName();
    private static final String BASE_URL = "https://api.zonky.cz/";

    private static Retrofit retrofit;
    private static Converter<ResponseBody, ZonkyAPIError> responseBodyConverter;
    private ZonkyService zonkyService;

    public ZonkyClient() {

        EventBus.getDefault().register(this);

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        zonkyService = retrofit.create(ZonkyService.class);

        responseBodyConverter = ZonkyClient.retrofit.responseBodyConverter(ZonkyAPIError.class, new Annotation[0]);
    }

    @Subscribe
    public void loginUser(final UserLogin.Request evt) {

        Call<AuthToken> call = zonkyService.getAuthToken(evt.userName, evt.password, "password", "SCOPE_APP_WEB");

        call.enqueue(new Callback<AuthToken>() {
            @Override
            public void onResponse(Call<AuthToken> call, Response<AuthToken> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ZonkySniperApplication.setAuthToken(response.body());
                    EventBus.getDefault().post(new UserLogin.Response(response.body()));
                } else {
                    resolveError(response, evt);
                }
            }

            @Override
            public void onFailure(Call<AuthToken> call, Throwable t) {

            }
        });
    }

    @Subscribe
    public void reloadMarket(final ReloadMarket.Request evt) {
        Call<List<Loan>> call = zonkyService.getNewLoansOnMarket("Bearer " + ZonkySniperApplication.getAuthToken().getAccess_token());

        call.enqueue(new Callback<List<Loan>>() {
            @Override
            public void onResponse(Call<List<Loan>> call, Response<List<Loan>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    EventBus.getDefault().post(new ReloadMarket.Response(response.body()));
                } else {
                    resolveError(response, evt);
                }
            }

            @Override
            public void onFailure(Call<List<Loan>> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    @Subscribe
    public void getWallet(final GetWallet.Request evt) {
        Call<Wallet> call = zonkyService.getWallet("Bearer " + ZonkySniperApplication.getAuthToken().getAccess_token());

        call.enqueue(new Callback<Wallet>() {
            @Override
            public void onResponse(Call<Wallet> call, Response<Wallet> response) {
                if (response.isSuccessful() && response.body() != null) {
                    EventBus.getDefault().post(new GetWallet.Response(response.body()));
                } else {
                    //resolveError(response, evt);
                }
            }

            @Override
            public void onFailure(Call<Wallet> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    @Subscribe
    public void invest(final Invest.Request evt) {
        Call<String> call = zonkyService.invest("Bearer " + ZonkySniperApplication.getAuthToken().getAccess_token(), evt.getInvestment());

        call.enqueue(new Callback<String>() {

            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    EventBus.getDefault().post(new Invest.Response());
                } else {
                    /*{
                        "error" : "insufficientBalance",
                        "uuid" : "1b92a6eb-6d96-4989-9e07-464795f6c845"
                     }
                    */
                    resolveError(response, evt);
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {

            }
        });
    }

    /************************************/
    /********* ERROR HANDLING ***********/
    /************************************/
    public static void resolveError(Response<?> response, Object evt) {
        if (response.errorBody() != null) {


            ZonkyAPIError error = new ZonkyAPIError();

            try {
                error = responseBodyConverter.convert(response.errorBody());
            } catch (IOException e) {
                //TODO what todo?
            }

            if ("invalid_token".equalsIgnoreCase(error.getError())) {
                if(ZonkySniperApplication.authFailed) { // pokud uz to jednou selhalo, vyhlas chybu klientovi
                    EventBus.getDefault().post(new UnresolvableError.Request(error));
                } else {
                    ZonkySniperApplication.authFailed = true;
                    ZonkySniperApplication.setAuthToken(null);
                    EventBus.getDefault().post(evt);
                }
            }

            if("insufficientBalance".equalsIgnoreCase(error.getError())) {
                EventBus.getDefault().post(new Invest.Failure(error.getError(), "Nemáte dostatek prostředků"));
            } else if("multipleInvestment".equalsIgnoreCase(error.getError()))  {
                EventBus.getDefault().post(new Invest.Failure(error.getError(), "Už jste investoval(a)"));
            }
        }
    }
}
