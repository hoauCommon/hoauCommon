package com.anji.hoau.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class MoneyUtil {

    private static BigDecimal value100=new BigDecimal(100);

    public static String getShowValue(Long money){
        return getBigDecimalValue(money).toString();
    }

    public static BigDecimal getBigDecimalValue(Long money){
        if(money==null )
            money=0L;
        BigDecimal result=new BigDecimal(money).divide(value100).setScale(2);
        return result;
    }

    public static String getShowValue(String money){
        return getShowValue(Long.valueOf(money));
    }

    public static String getShowValue(BigDecimal money){
        return getShowValue(money.longValue());
    }

    public static Long getValue(String showValue){
        if(StringUtils.isBlank(showValue))
            return null;
        BigDecimal bigDecimal=new BigDecimal(showValue).setScale(2,BigDecimal.ROUND_HALF_UP);
        return getValue(bigDecimal);

    }



    public static Long getValue(BigDecimal showValue){
        if(showValue==null)
            return null;
        return showValue.multiply(value100).longValue();
    }


    public static void main(String[] args){
      //  System.out.println(Double.valueOf(String.format("%d.%2f",10,1)).toString());
        String value=MoneyUtil.getShowValue(101L);
        System.out.println(value);
        System.out.print(MoneyUtil.getValue(value));

    }




}
