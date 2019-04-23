package cn.no7player.util;

import javax.servlet.http.HttpServletRequest;

public class IPUtils {

    /**
     * 获取用户实际ip,有nginx代理的情况
     * @param request
     * @return
     */
    public static String getIP(HttpServletRequest request) {
        String ipAddress = request.getHeader("x-forwarded-for");
        if (ipAddress != null) {
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < ipAddress.length(); i++) {
                char ch = ipAddress.charAt(i);
                if (ch != ' ')
                    buf.append(ch);
            }
            ipAddress = buf.toString();
            System.out.println("getIp x-forwarded-for");
        }

        if (ipAddress != null) {
            if (ipAddress.length() > 0 && !ipAddress.startsWith("10.")) {
                int tmpIndex = ipAddress.indexOf(",");
                if (tmpIndex > 0) {
                    ipAddress = ipAddress.substring(0, tmpIndex);
                }
                System.out.println("getIp 10.");
                return ipAddress;
            }
        }

        ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (ipAddress != null) {
            int index = ipAddress.indexOf(',');
            if (index > 0) {
                ipAddress = ipAddress.substring(0, index);
            }
            System.out.println("getIp HTTP_X_FORWARDED_FOR");
            return ipAddress;
        }

        /*
         * ipAddress = request.getHeader("CLIENT_IP"); if (ipAddress == null) {
         * return request.getRemoteAddr(); } else { return ipAddress; }
         */
        ipAddress = request.getHeader("X-Real-IP");
        System.out.println("getIp X-Real-IP");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
            System.out.println("getIp X-Real-IP");
        }
        int index = ipAddress.indexOf(',');
        if (index > 0) {
            ipAddress = ipAddress.substring(0, index);
        }
        return ipAddress;
    }

}
