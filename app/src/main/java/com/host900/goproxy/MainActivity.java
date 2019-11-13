package com.host900.goproxy;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import snail007.proxysdk.LogCallback;
import snail007.proxysdk.Proxysdk;

public class MainActivity extends AppCompatActivity {

    String TAG = "HomeFragment";
    String serviceID = "srv";
    int log_line_cnt = 0;

    EditText log  ;

    Handler handler=new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String line=msg.getData().getString("line");
            if (++log_line_cnt > 100) {
                log.setText("");
            }
            log.append(line + "\n");
        }
    };

    public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText editText = findViewById(R.id.input);
        SharedPreferences config = getSharedPreferences("config", Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = config.edit();

        final String args = config.getString("args", "");
        editText.setText(args);
        editText.addTextChangedListener(watcher(editor, editText));

        TextView status = findViewById(R.id.tv_status);
        log = (EditText) findViewById(R.id.log_output);
        TextView tip = findViewById(R.id.tip);
        TextView ipaddrs = findViewById(R.id.ip_addrs);
        String sdkVersion = Proxysdk.version();
        TextView viewManual = findViewById(R.id.view_manual);
        TextView joinQQ = findViewById(R.id.join_qq_group);
        ipaddrs.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                  ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                 ClipData mClipData = ClipData.newPlainText("ip", ((TextView)view).getText());
                 cm.setPrimaryClip(mClipData);
                Toast.makeText(view.getContext(), "IP已经复制", Toast.LENGTH_LONG).show();
                return false;
            }
        });
        joinQQ.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData mClipData = ClipData.newPlainText("qq", ((TextView)view).getText());
                cm.setPrimaryClip(mClipData);
                Toast.makeText(view.getContext(), "QQ群号码已复制", Toast.LENGTH_LONG).show();
                return false;
            }
        });
        //ui
        viewManual.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG); //下划线
        viewManual.getPaint().setAntiAlias(true);//抗锯齿

        getSupportActionBar().setTitle("全能代理服务器");
        tip.setText("由 snail007/goproxy SDK " + sdkVersion + " 强力驱动！");
        ipaddrs.setText(getIpAddress(getBaseContext()));

        joinQQ.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG); //下划线
        joinQQ.getPaint().setAntiAlias(true);//抗锯齿

        log.setMovementMethod(ScrollingMovementMethod.getInstance());

        //event
        findViewById(R.id.btn_start).setOnClickListener(start(log, status, editText, this));
        findViewById(R.id.btn_stop).setOnClickListener(stop(status, editText));
        viewManual.setOnClickListener(openURL("https://snail007.github.io/goproxy/manual/zh/#/"));
//        joinQQ.setOnClickListener(openURL("https://jq.qq.com/?_wv=1027&k=5G2EwxR"));

        return;
    }

    public View.OnClickListener stop(final TextView status, final EditText editText) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Proxysdk.stop((serviceID));
                editText.setEnabled(true);
                status.setText("已停止");
            }
        };
    }

    public View.OnClickListener start(final EditText log, final TextView status, final EditText editText, final Context ctx) {

        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String args = editText.getText().toString().trim();
                if (args.indexOf("proxy") == 0 && args.length() >= 5) {
                    args = args.substring(5);
                }
                if (args.replaceAll("\n","").length() == 0) {
                    Toast.makeText(ctx, "参数不能为空", Toast.LENGTH_LONG).show();
                    return;
                }
                String err = Proxysdk.startWithLog(serviceID, args, "", new LogCallback() {
                    @Override
                    public void write(String line) {
                        Message msg=Message.obtain();
                        msg.what=1;
                        Bundle bundle=new Bundle();
                        bundle.putString("line", line);
                       msg.setData(bundle);
                        handler.sendMessage(msg);
                    }
                });
                if (!err.isEmpty()) {
                    Toast.makeText(ctx, err, Toast.LENGTH_LONG).show();
                    //  Log.d(TAG, err);
                } else {
                    editText.setEnabled(false);
                    status.setText("运行中");
                }
            }
        };
    }

    public View.OnClickListener openURL(final String u) {

        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                Uri content_url = Uri.parse(u);
                intent.setData(content_url);
//                startActivity(Intent.createChooser(intent, "请选择浏览器"));
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Proxysdk.stop(serviceID);
    }

    public TextWatcher watcher(final SharedPreferences.Editor editor, final EditText editText) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                editor.putString("args", editText.getText().toString());
                // Log.d(TAG, editText.getText().toString());
                editor.commit();
            }
        };
    }

    public static String getIpAddress(Context context) {
        NetworkInfo info = ((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            // 3/4g网络
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                try {
                    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                        NetworkInterface intf = en.nextElement();
                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                                return inetAddress.getHostAddress();
                            }
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }

            } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                //  wifi网络
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String ipAddress = intIP2StringIP(wifiInfo.getIpAddress());
                return ipAddress;
            } else if (info.getType() == ConnectivityManager.TYPE_ETHERNET) {
                // 有限网络
                return getLocalIp();
            }
        }
        return null;
    }

    private static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }


    // 获取有限网IP
    private static String getLocalIp() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()
                            && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {

        }
        return "0.0.0.0";
    }
}