package com.sinovoice.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.util.Log;
/**
 * ��assetsĿ¼�µ�AccountInfo.txt��ȡ������Ϣ
 * @author sinovoice
 *
 */
public class AccountInfo {

    private static AccountInfo mInstance;

    private Map<String, String> mAccountMap;

    private AccountInfo() {
        mAccountMap = new HashMap<String, String>();
    }

    public static AccountInfo getInstance() {
        if (mInstance == null) {
            mInstance = new AccountInfo();
        }
        return mInstance;
    }

    /**
     * ��ȡcapkey��Ϣ������Ϊasr.local.grammar.v4���ƶ�Ϊasr.cloud.freetalk����Ҫ�����ƿ�����������ѡ��Ӧ�������ſ���ʹ�ã�����᷵�ش���12
     * @return	����capkey���ַ���
     */
    public String getCapKey(){
        return mAccountMap.get("capKey");
    }
    /**
     * ��ȡdeveloperKey��Ϣ��Ϊ��������Ϣ����Ҫ�ӿ����������½�Ӧ�ã���Ӧ�������в鿴��
     * @return	����developerKey���ַ���
     */
    public String getDeveloperKey(){
        return mAccountMap.get("developerKey");
    }
    /**
     * ��ȡappKey��Ϣ��ΪӦ����Ϣ����Ҫ�ӿ����������½�Ӧ�ã���Ӧ�������в鿴��
     * @return	����appKey���ַ���
     */
    public String getAppKey(){
        return mAccountMap.get("appKey");
    }
    /**
     * ��ȡʹ�õ�Url��Ϣ����Ҫ�ӿ����������½�Ӧ�ã���Ӧ�������в鿴������Ӧ�õ�url��ַΪhttp://test.api.hcicloud.com:8888
     * ����������ã���Ҫ������ƽ̨�������룬����ɹ����url��ַ���б䶯���뿪����ע�⡣
     * @return	����Url���ַ���
     */
    public String getCloudUrl(){
        return mAccountMap.get("cloudUrl");
    }
    
    /**
     * ����assetsĿ¼�µ�AccountInfo.txt�ļ�
     * @param context	������
     * @return	�ɹ�����true��ʧ�ܷ���false
     */
    public boolean loadAccountInfo(Context context) {
        boolean isSuccess = true;
        try {
            InputStream in = null;
            in = context.getResources().getAssets().open("AccountInfo.txt");
            InputStreamReader inputStreamReader = new InputStreamReader(in,
                    "utf-8");
            BufferedReader br = new BufferedReader(inputStreamReader);
            String temp = null;
            String[] sInfo = new String[2];
            temp = br.readLine();
            while (temp != null) {
                if (!temp.startsWith("#") && !temp.equalsIgnoreCase("")) {
                    sInfo = temp.split("=");
                    if (sInfo.length == 2){
                        if(sInfo[1] == null || sInfo[1].length() <= 0){
                            isSuccess = false;
                            Log.e("AccountInfo", sInfo[0] + "is null");
                            break;
                        }
                        mAccountMap.put(sInfo[0], sInfo[1]);
                    }
                }
                temp = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            isSuccess = false;
        }
        
        return isSuccess;
    }
    

}
