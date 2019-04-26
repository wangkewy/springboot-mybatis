package cn.no7player.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class TimeUtils {

    /**
     * 取得系统时间
     * @param pattern eg:yyyy-MM-dd HH:mm:ss,SSS
     * @return
     */
    public static String getSysTime(String pattern) {

        return formatSysTime(new SimpleDateFormat(pattern));
    }

    /**
     * 格式化系统时间
     * @param format
     * @return
     */
    private static String formatSysTime(SimpleDateFormat format) {

        String str = format.format(Calendar.getInstance().getTime());
        return str;
    }

    /**
     * 日期转化 2019年4月24日1时
     * */
    public static String formatDate(String date){
        date = date.substring(0, date.indexOf("日"));
        StringBuffer sbf = new StringBuffer();
        //年
        String[] strY = date.split("年");
        sbf.append(strY[0]);
        //月
        String[] strM = strY[1].split("月");
        String month = strM[0];
        if(month.length() == 1){
            sbf.append("0");
        }
        sbf.append(month);
        //日
        String[] strD = strM[1].split("日");
        String day = strD[0];
        if(day.length() == 1){
            sbf.append("0");
        }
        sbf.append(day);
        //时分
        sbf.append("0808");

        return sbf.toString();
    }

}