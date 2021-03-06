package eu.urbancoders.zonkysniper.core;

import eu.urbancoders.zonkysniper.dataobjects.Rating;
import eu.urbancoders.zonkysniper.dataobjects.RepaymentCalendar;
import eu.urbancoders.zonkysniper.dataobjects.RepaymentCalendarItem;

/**
 * Vypocty splatkovych kalendaru, urokovych sazeb apod.
 *
 * Author: Ondrej Steger (ondrej@steger.cz)
 * Datum: 22.09.2017
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

public class LoanCalculator {

    /**
     * Vypocitat splatkovy kalendar investice
     *
     * @param p - Principal
     * @param iy - Interest rate
     * @param nm - months
     * @param feeRate - procenta poplatku
     */
    public static RepaymentCalendar calculateAmortization(double p, double iy, int nm, double feeRate) {

        RepaymentCalendar cal = new RepaymentCalendar(nm, p);

        double newbal;
        double im = (iy / 12) / 100;
        double mp, ip, pp;
        double fee;
        double revenue;
        int i;

        mp = p * im * Math.pow(1 + im, (double) nm) / (Math.pow(1 + im, (double) nm) - 1);
        revenue =- p;
        //print amortization schedule for all months except the last month
        for (i = 1; i < nm; i++) {
            ip = p * im;//interest paid
            pp = mp - ip; //princial paid
            newbal = p - pp; //new balance
            fee = feeRate / 100 / 12 * p;  // poplatek
            revenue = revenue + (mp - fee);
            cal.addItem(new RepaymentCalendarItem(i, p, newbal, mp, ip, pp, fee, revenue));
            p = newbal;  //update old balance
        }
        //last month
        pp = p;
        ip = p * im;
        mp = pp + ip;
        newbal = 0.0;
        fee = 0;
        revenue = revenue + (mp - fee);
        cal.addItem(new RepaymentCalendarItem(i, p, newbal, mp, ip, pp, fee, revenue));

        return cal;
    }

    /**
     * Výpočet čisté úrokové sazby se zohledněním rizikových nákladů
     * @param rating
     */
    public static double calculateNetInterestRate(Rating rating) {
        return rating.getInterestRate() - rating.getRiskCost() - rating.getFeeRate();
    }
}
