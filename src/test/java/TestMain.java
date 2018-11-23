import com.tiza.util.CommonUtil;
import org.junit.Test;

import java.io.*;

/**
 * Description: TestMain
 * Author: DIYILIU
 * Update: 2018-11-22 14:10
 */
public class TestMain {


    @Test
    public void test() throws Exception {
        String ss = "INSERT INTO `obd_support`.`obd_reserve_cmd` ( `device_id`, `send_hex`,) VALUES ( '46055101839', '0006001E0E10000A');";
        //System.out.println(CommonUtil.bytesToStr(CommonUtil.packSIM("46055101839", 6)));

        File file = new File("C:\\Users\\DIYILIU\\Desktop\\临时.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

        String sql = "INSERT INTO `obd_support`.`obd_reserve_cmd` ( `device_id`, `send_hex`) VALUES ( '";
        for (String device = reader.readLine(); device != null; ) {
            String insert = sql + device + "','015A" + CommonUtil.bytesToStr(CommonUtil.packSIM(device, 6)) + "0006001E0E10000A');";
            System.out.println(insert);
            device = reader.readLine();
        }
    }


    @Test
    public void test1(){

        String str = "FAFAC77F000AB97F53FF000100D9FB";

        System.out.println(str.length());

        System.out.println(str.substring(24, 26));
    }

    @Test
    public void test2(){

        String str = "gateway:192.168.1.132:9008";

        System.out.println(str.substring(str.indexOf(":") + 1));

    }
}
