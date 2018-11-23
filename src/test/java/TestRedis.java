import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Description: TestRedis
 * Author: DIYILIU
 * Update: 2018-11-22 14:46
 */
public class TestRedis {


    @Test
    public void test() {

        JedisPool jedisPool = new JedisPool("192.168.1.156", 6379);

        Jedis jedis = jedisPool.getResource();
        String str = jedis.get("obd:t:46061802495");

        System.out.println(str);
    }

    @Test
    public void test1(){
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(100);
        config.setMaxIdle(50);
        config.setMaxWaitMillis(3000);

        JedisPool jedisPool = new JedisPool(config, "192.168.1.156", 6379);
        Jedis jedis = jedisPool.getResource();
        jedis.auth("");

        String str = jedis.get("123");
        System.out.println(str);
    }


    @Test
    public void testPost() throws Exception {
        String url = "http://localhost:3280/city/criteria";

        // 定义HttpClient
        HttpClient client = new DefaultHttpClient();

        HttpPost request = new HttpPost();
        request.setURI(new URI(url));
        //设置参数
        List<NameValuePair> list = new ArrayList();
        list.add(new BasicNameValuePair("prefix", "苏C"));
        request.setEntity(new UrlEncodedFormEntity(list, Charset.forName("UTF-8")));

        // 连接时间
        client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
        // 数据传输时间
        client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 3000);

        HttpResponse response = client.execute(request);
        HttpEntity entity = response.getEntity();
        String result = EntityUtils.toString(entity, "UTF-8");
        System.out.println(result);
    }
}
