package com.sinovoice.example.asrrecorder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.sinovoice.example.AccountInfo;
import com.sinovoice.hcicloudsdk.android.asr.recorder.ASRRecorder;
import com.sinovoice.hcicloudsdk.api.HciCloudSys;
import com.sinovoice.hcicloudsdk.common.AuthExpireTime;
import com.sinovoice.hcicloudsdk.common.HciErrorCode;
import com.sinovoice.hcicloudsdk.common.InitParam;
import com.sinovoice.hcicloudsdk.common.asr.AsrConfig;
import com.sinovoice.hcicloudsdk.common.asr.AsrGrammarId;
import com.sinovoice.hcicloudsdk.common.asr.AsrInitParam;
import com.sinovoice.hcicloudsdk.common.asr.AsrRecogResult;
import com.sinovoice.hcicloudsdk.recorder.ASRCommonRecorder;
import com.sinovoice.hcicloudsdk.recorder.ASRRecorderListener;
import com.sinovoice.hcicloudsdk.recorder.RecorderEvent;

/**
 * Asr¼�������ò������ʾDemo
 * 
 * @author sinovoice
 */
public class HciCloudAsrRecorderActivity extends Activity {
	private static final String TAG = "HciCloudAsrRecorderActivity";

    /**
     * �����û���Ϣ������
     */
    private AccountInfo mAccountInfo;

	private TextView mResult;
	private TextView mState;
	private TextView mError;
	private ListView mGrammarLv;
	private Button mBtnRecog;
	private Button mBtnRecogRealTimeMode;

	private ASRRecorder mAsrRecorder;

	private String grammar = null;

	private static class WeakRefHandler extends Handler {
		private WeakReference<HciCloudAsrRecorderActivity> ref = null;

		public WeakRefHandler(HciCloudAsrRecorderActivity activity) {
			ref = new WeakReference<HciCloudAsrRecorderActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			if (ref.get() != null) {
				switch (msg.arg1) {
				case 1:
					if (!msg.obj.toString().equalsIgnoreCase(""))
						ref.get().mState.setText(msg.obj.toString());
					break;
				case 2:
					if (!msg.obj.toString().equalsIgnoreCase(""))
						ref.get().mResult.setText(msg.obj.toString());
					break;
				case 3:
					if (!msg.obj.toString().equalsIgnoreCase(""))
						ref.get().mError.setText(msg.obj.toString());
					break;
				default:
					break;
				}
			}
		}
	}

	private static Handler mUIHandle = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);

		mResult = (TextView) findViewById(R.id.resultview);
		mState = (TextView) findViewById(R.id.stateview);
		mError = (TextView) findViewById(R.id.errorview);
		mGrammarLv = (ListView) findViewById(R.id.grammar_list);
		mBtnRecogRealTimeMode = (Button) findViewById(R.id.begin_recog_real_time_mode);
		mBtnRecog = (Button) findViewById(R.id.beginrecog);

		mUIHandle = new WeakRefHandler(this);
				
        mAccountInfo = AccountInfo.getInstance();
        boolean loadResult = mAccountInfo.loadAccountInfo(this);
        if (loadResult) {
            // ������Ϣ�ɹ�����������
			Toast.makeText(getApplicationContext(), "���������˺ųɹ�",
					Toast.LENGTH_SHORT).show();
        } else {
            // ������Ϣʧ�ܣ���ʾʧ�ܽ���
			Toast.makeText(getApplicationContext(), "���������˺�ʧ�ܣ�����assets/AccountInfo.txt�ļ�����д��ȷ�������˻���Ϣ���˻���Ҫ��www.hcicloud.com������������ע�����롣",
					Toast.LENGTH_SHORT).show();
            return;
        }
        
     // ������Ϣ,����InitParam, ������ò������ַ���
        InitParam initParam = getInitParam();
        String strConfig = initParam.getStringConfig();
        Log.i(TAG,"\nhciInit config:" + strConfig);
        
        // ��ʼ��
        int errCode = HciCloudSys.hciInit(strConfig, this);
        if (errCode != HciErrorCode.HCI_ERR_NONE && errCode != HciErrorCode.HCI_ERR_SYS_ALREADY_INIT) {
        	Toast.makeText(getApplicationContext(), "hciInit error: " + HciCloudSys.hciGetErrorInfo(errCode),Toast.LENGTH_SHORT).show();
            return;
        } 
        
        // ��ȡ��Ȩ/������Ȩ�ļ� :
        errCode = checkAuthAndUpdateAuth();
        if (errCode != HciErrorCode.HCI_ERR_NONE) {
            // ����ϵͳ�Ѿ���ʼ���ɹ�,�ڽ���ǰ��Ҫ���÷���hciRelease()����ϵͳ�ķ���ʼ��
        	Toast.makeText(getApplicationContext(), "CheckAuthAndUpdateAuth error: " + HciCloudSys.hciGetErrorInfo(errCode),Toast.LENGTH_SHORT).show();
            HciCloudSys.hciRelease();
            return;
        }
		
		// ��ȡ�û��ĵ��õ�����
		String capKey = mAccountInfo.getCapKey();
		
//		if (!capKey.equals("asr.cloud.grammar"))
//		{
//			mBtnRecogRealTimeMode.setEnabled(true);
//		}

		// ��ʼ��¼����
		mAsrRecorder = new ASRRecorder();

		// ¼������ʼ����������
		AsrInitParam asrInitParam = new AsrInitParam();
		String dataPath = getFilesDir().getPath().replace("files", "lib");
		asrInitParam.addParam(AsrInitParam.PARAM_KEY_INIT_CAP_KEYS, capKey);
		asrInitParam.addParam(AsrInitParam.PARAM_KEY_DATA_PATH, dataPath);
		asrInitParam.addParam(AsrInitParam.PARAM_KEY_FILE_FLAG, AsrInitParam.VALUE_OF_PARAM_FILE_FLAG_ANDROID_SO);
		Log.v(TAG, "init parameters:" + asrInitParam.getStringConfig());

		// ¼������ʼ��
		mAsrRecorder.init(asrInitParam.getStringConfig(), new ASRResultProcess());

		// ����ʶ�����
		final AsrConfig asrConfig = new AsrConfig();
		// PARAM_KEY_CAP_KEY ����ʹ�õ�����
		asrConfig.addParam(AsrConfig.SessionConfig.PARAM_KEY_CAP_KEY, capKey);
		// PARAM_KEY_AUDIO_FORMAT ��Ƶ��ʽ���ݲ�ͬ������ʹ�ò��õ���Ƶ��ʽ
		asrConfig.addParam(AsrConfig.AudioConfig.PARAM_KEY_AUDIO_FORMAT,
				AsrConfig.AudioConfig.VALUE_OF_PARAM_AUDIO_FORMAT_PCM_16K16BIT);
		// PARAM_KEY_ENCODE ��Ƶ����ѹ����ʽ��ʹ��OPUS������Ч��С��������
		asrConfig.addParam(AsrConfig.AudioConfig.PARAM_KEY_ENCODE, AsrConfig.AudioConfig.VALUE_OF_PARAM_ENCODE_SPEEX);
		// �������ã��˴�����ȫ��ѡȡȱʡֵ
 
		// �﷨��ص�����,��ʹ������˵�������Բ������ø���
		if (capKey.contains("local.grammar")) {
			grammar = loadGrammar("stock_10001.gram");
			// ���ر����﷨��ȡ�﷨ID
			AsrGrammarId id = new AsrGrammarId();
			ASRCommonRecorder.loadGrammar("capkey=" + capKey +",grammarType=jsgf", grammar, id);
			Log.d(TAG, "grammarid="+id);
			// PARAM_KEY_GRAMMAR_TYPE �﷨���ͣ�ʹ������˵����ʱ���������´˲���
			asrConfig.addParam(AsrConfig.GrammarConfig.PARAM_KEY_GRAMMAR_TYPE,
					AsrConfig.GrammarConfig.VALUE_OF_PARAM_GRAMMAR_TYPE_ID);
			asrConfig.addParam(AsrConfig.GrammarConfig.PARAM_KEY_GRAMMAR_ID,
					"" + id.getGrammarId());

			List<String> grammarList = loadGrammarList(grammar);
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_list_item_1, grammarList);
			mGrammarLv.setAdapter(adapter);
		}
//		else if(capKey.contains("cloud.grammar")) {
//			grammar = loadGrammar("stock_10001.gram");
//			// PARAM_KEY_GRAMMAR_TYPE �﷨���ͣ�ʹ������˵����ʱ���������´˲���
//			asrConfig.addParam(AsrConfig.GrammarConfig.PARAM_KEY_GRAMMAR_TYPE,
//					AsrConfig.GrammarConfig.VALUE_OF_PARAM_GRAMMAR_TYPE_JSGF);
//
//			List<String> grammarList = loadGrammarList(grammar);
//			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
//					android.R.layout.simple_list_item_1, grammarList);
//			mGrammarLv.setAdapter(adapter);
//		}

		Log.v(TAG, "asr config:" + asrConfig.getStringConfig());

		//��ʵʱʶ��ť
		mBtnRecog.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (mAsrRecorder.getRecorderState() == ASRRecorder.RECORDER_STATE_IDLE) {
					asrConfig.addParam(AsrConfig.SessionConfig.PARAM_KEY_REALTIME, "no");
					mAsrRecorder.start(asrConfig.getStringConfig(), grammar);
				} else {
					Log.e("recorder", "¼����δ���ڿ���״̬�����Ե�");
				}
			}
		});

		//ʵʱʶ��İ�ť
		mBtnRecogRealTimeMode.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mAsrRecorder.getRecorderState() == ASRRecorder.RECORDER_STATE_IDLE) {
					asrConfig.addParam(AsrConfig.SessionConfig.PARAM_KEY_REALTIME, "yes");
					mAsrRecorder.start(asrConfig.getStringConfig(), grammar);
				} else {
					Log.e("recorder", "¼����δ���ڿ���״̬�����Ե�");
				}
			}
		});
	}

	/**
	 * ��·���¶�ȡ�﷨�ļ�
	 * @param fileName	grammar�ļ���·��
	 * @return	��ȡ��grammar�ļ�����
	 */
	private String loadGrammar(String fileName) {
		String grammar = "";
		try {
			InputStream is = null;
			try {
				is = getAssets().open(fileName);
				byte[] data = new byte[is.available()];
				is.read(data);
				grammar = new String(data);
			} finally {
				is.close();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return grammar;
	}

	/**
	 * ����grammar����
	 * @param WordlistGrammar	grammar����������
	 * @return	���ص�List�б���
	 */
	private List<String> loadGrammarList(String WordlistGrammar) {

		List<String> strList = new ArrayList<String>();

		for (String msg : WordlistGrammar.split("\n")) {
			strList.add(msg.trim());
		}

		return strList;
	}

	/**
	 * ASR¼�����ص���
	 * @author sinovoice
	 *
	 */
	private class ASRResultProcess implements ASRRecorderListener {
		//����ص�
		@Override
		public void onRecorderEventError(RecorderEvent arg0, int arg1) {
			String sError = "������Ϊ��" + arg1;
			Message m = mUIHandle.obtainMessage(1, 3, 1, sError);
			mUIHandle.sendMessage(m);
		}

		//ʶ�����ص�
		@Override
		public void onRecorderEventRecogFinsh(RecorderEvent recorderEvent,
				AsrRecogResult arg1) {
			if (recorderEvent == RecorderEvent.RECORDER_EVENT_RECOGNIZE_COMPLETE) {
				String sState = "״̬Ϊ��ʶ�����";
				Message m = mUIHandle.obtainMessage(1, 1, 1, sState);
				mUIHandle.sendMessage(m);
			}
			if (arg1 != null) {
				String sResult;
				if (arg1.getRecogItemList().size() > 0) {
					//ʶ����
					sResult = "ʶ����Ϊ��" + arg1.getRecogItemList().get(0).getRecogResult();
					//���Ŷȣ�����ֵ��asr.local.grammar.v4��Ч���÷ִ���30ʱ��Ϊʶ����ȷ���÷�С��30��Ϊ�������������п�����������
					//�����asr.cloud.freetalk ��ֵ���Բ��ù��ġ�
					int score = arg1.getRecogItemList().get(0).getScore();
					sResult += "\t���Ŷ�Ϊ��" + score;
				} else {
					sResult = "δ����ȷʶ��,����������";
				}
				Message m = mUIHandle.obtainMessage(1, 2, 1, sResult);
				mUIHandle.sendMessage(m);
			}
		}

		//¼����״̬�ص�
		@Override
		public void onRecorderEventStateChange(RecorderEvent recorderEvent) {
			String sState = "״̬Ϊ����ʼ״̬";
			if (recorderEvent == RecorderEvent.RECORDER_EVENT_BEGIN_RECORD) {
				sState = "״̬Ϊ����ʼ¼��";
			} else if (recorderEvent == RecorderEvent.RECORDER_EVENT_BEGIN_RECOGNIZE) {
				sState = "״̬Ϊ����ʼʶ��";
			} else if (recorderEvent == RecorderEvent.RECORDER_EVENT_NO_VOICE_INPUT) {
				sState = "״̬Ϊ������Ƶ����";
			}
			Message m = mUIHandle.obtainMessage(1, 1, 1, sState);
			mUIHandle.sendMessage(m);
		}

		//¼�����ݻص�������ͨ���˺���������¼������
		@Override
		public void onRecorderRecording(byte[] volumedata, int volume) {
		}

		//ʶ����̻ص���Ŀǰ�˻ص�ֻ��ʵʱ������Ч
		@Override
		public void onRecorderEventRecogProcess(RecorderEvent recorderEvent,
				AsrRecogResult arg1) {
			// TODO Auto-generated method stub
			if (recorderEvent == RecorderEvent.RECORDER_EVENT_RECOGNIZE_PROCESS) {
				String sState = "״̬Ϊ��ʶ���м䷴��";
				Message m = mUIHandle.obtainMessage(1, 1, 1, sState);
				mUIHandle.sendMessage(m);
			}
			if (arg1 != null) {
				String sResult;
				if (arg1.getRecogItemList().size() > 0) {
					sResult = "ʶ���м������Ϊ��"
							+ arg1.getRecogItemList().get(0).getRecogResult();
				} else {
					sResult = "δ����ȷʶ��,����������";
				}
				Message m = mUIHandle.obtainMessage(1, 2, 1, sResult);
				mUIHandle.sendMessage(m);
			}
		}
	}

	
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
        mAsrRecorder.release();
        HciCloudSys.hciRelease();
        Log.i(TAG, "onDestroy()");
    }
	
	
	/**
     * �����Ȩ�Ƿ���Ҫ���£��˺��������Ȩ�����ڣ������hciCheckAuth���������������£������Ȩ���ڣ�������Ȩ�Ƿ���ڣ�û�й��ھ������ˡ�
     * @return true �ɹ�
     */
    private int checkAuthAndUpdateAuth() {
        
    	// ��ȡϵͳ��Ȩ����ʱ��
        int initResult;
        AuthExpireTime objExpireTime = new AuthExpireTime();
        initResult = HciCloudSys.hciGetAuthExpireTime(objExpireTime);
        if (initResult == HciErrorCode.HCI_ERR_NONE) {
            // ��ʾ��Ȩ����,���û�����Ҫ��ע��ֵ,�˴�����ɺ���
            Date date = new Date(objExpireTime.getExpireTime() * 1000);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
            Log.i(TAG, "expire time: " + sdf.format(date));

            if (objExpireTime.getExpireTime() * 1000 > System.currentTimeMillis()) {
                // �Ѿ��ɹ���ȡ����Ȩ,���Ҿ�����Ȩ�����г����ʱ��(>7��)
                Log.i(TAG, "checkAuth success");
                return initResult;
            }
            
        } 
        
        // ��ȡ����ʱ��ʧ�ܻ����Ѿ�����
        initResult = HciCloudSys.hciCheckAuth();
        if (initResult == HciErrorCode.HCI_ERR_NONE) {
            Log.i(TAG, "checkAuth success");
            return initResult;
        } else {
            Log.e(TAG, "checkAuth failed: " + initResult);
            return initResult;
        }
    }
    
    /**
     * ���س�ʼ����Ϣ�����е�key��Ϣ��Ҫ�ӿ���������ע��Ӧ�ã���Ӧ�������л�ȡ�������õ�assetsĿ¼�µ�AccountInfo.txt�ļ���
     * 
     * @param context �������ﾳ
     * @return ϵͳ��ʼ������
     */
    private InitParam getInitParam() {
        String authDirPath = this.getFilesDir().getAbsolutePath();

        // ǰ����������
        InitParam initparam = new InitParam();

        // ��Ȩ�ļ�����·�����������
        initparam.addParam(InitParam.AuthParam.PARAM_KEY_AUTH_PATH, authDirPath);

        // �Ƿ��Զ���������Ȩ,��� ��ȡ��Ȩ/������Ȩ�ļ���ע��
        initparam.addParam(InitParam.AuthParam.PARAM_KEY_AUTO_CLOUD_AUTH, "no");

        // �����Ʒ���Ľӿڵ�ַ���������
        initparam.addParam(InitParam.AuthParam.PARAM_KEY_CLOUD_URL, AccountInfo.getInstance().getCloudUrl());

        // ������Key���������ɽ�ͨ�����ṩ
        initparam.addParam(InitParam.AuthParam.PARAM_KEY_DEVELOPER_KEY, AccountInfo.getInstance().getDeveloperKey());

        // Ӧ��Key���������ɽ�ͨ�����ṩ
        initparam.addParam(InitParam.AuthParam.PARAM_KEY_APP_KEY, AccountInfo.getInstance().getAppKey());

        // ������־����
        String sdcardState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(sdcardState)) {
            String sdPath = Environment.getExternalStorageDirectory()
                    .getAbsolutePath();
            String packageName = this.getPackageName();

            String logPath = sdPath + File.separator + "sinovoice"
                    + File.separator + packageName + File.separator + "log"
                    + File.separator;

            // ��־�ļ���ַ
            File fileDir = new File(logPath);
            if (!fileDir.exists()) {
                fileDir.mkdirs();
            }

            // ��־��·������ѡ�������������Ϊ����������־
            initparam.addParam(InitParam.LogParam.PARAM_KEY_LOG_FILE_PATH, logPath);

            // ��־��Ŀ��Ĭ�ϱ������ٸ���־�ļ��������򸲸���ɵ���־
            initparam.addParam(InitParam.LogParam.PARAM_KEY_LOG_FILE_COUNT, "5");

            // ��־��С��Ĭ��һ����־�ļ�д��󣬵�λΪK
            initparam.addParam(InitParam.LogParam.PARAM_KEY_LOG_FILE_SIZE, "1024");

            // ��־�ȼ���0=�ޣ�1=����2=���棬3=��Ϣ��4=ϸ�ڣ�5=���ԣ�SDK�����С�ڵ���logLevel����־��Ϣ
            initparam.addParam(InitParam.LogParam.PARAM_KEY_LOG_LEVEL, "5");
        }

        return initparam;
    }

}
