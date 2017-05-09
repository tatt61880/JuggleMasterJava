import java.awt.*;
import java.io.*;
import java.net.*;
import java.applet.AudioClip;
import javax.swing.JApplet;

public class Jmj extends JApplet implements Runnable{
	private static final long serialVersionUID = -1153293381918719463L;
	String strVer = "2.22";   // Version No
	boolean TEST_MODE = false; // Test mode : true, Release mode : false
	public static String dirPath;

	public final static float KW = 0.25f;
	public final static int XR = 1024;
	public final static int DW = 290;

	public final static int BALL_NUM_MAX = 35;
	public final static int LMAX = 200;
	public final static int MMAX = 11;
	public final static String NORMAL = "Normal";

	public final static int PERMIN = 1;  // person's number min
	public final static int PERMAX = 10;  // person's number max
	static int iPerNo = 2;   // number of person
	private final float redrawRate = 100.0f;

	static int iPerMax = iPerNo;

	// person's position
	int iXData[] = new int[100];
	int iYData[] = new int[100];

	public final static int PXY = 30;

	int iMoveX = 0; // X,Y方向に全体をずらす座標補正
	int iXmin, iXmax;  // area of display in gg2

	public enum State{
		IDLE,
		PAUSE,
		JUGGLING
	}
	final static int SITESWAP_MODE = -3; // should be < -1;
	final static int MOTION_MODE = -2; // should be < -1;
	final static int FORMATION_MODE = -1; // should be < -1;

	static int Y_OFFSET = 0;
	ImageFrame imf = null;
	JmjController controller;
	PatternHolder holder = null;
	Thread kicker = null;
	boolean isActive = false;

	int dpm;
	int hand_x;
	int hand_y;
	int arm_x;
	int arm_y;
	int gx_max, gx_min;
	int tx, c0;

	State status;
	Arm ap[] = new Arm[PERMAX];
	int gy_max, gy_min;

	float ga = 9.8f;
	float dwell, height;
	float speed = 1.0f;
	int base;
	boolean mirror = false;
	String pattern;
	String siteswap;
	String motion = NORMAL;
	String startpattern = "Throw Twice";
	String patternfiles = "pattern.jm/pattern_ja.jm";
	String filename;
	byte motionarray[] = {13, 0, 4, 0};

	byte motionarray2[][] = new byte[PERMAX][1000];
	int motionlength[] = new int[PERMAX];

	String motion2[] = new String[PERMAX];    // ダミー あとで正しくすること

	public final static String FORMATION_BASIC = "1-Person";

	int formationXY[] = new int[100];   // person's formation position (X & Y);
	String formation = FORMATION_BASIC;
	int formationarray[] = {0, 0};  // 1 person (default)

	int startindex = -1;
	int fallback_startindex = 0;

	long timeCount;
	int time_period;

	boolean isSync = false;
	boolean showSS = true;
	boolean hand_on = true;
	boolean bSound = false;
	int intsyn;

	Ball rhand[] = new Ball[PERMAX], lhand[] = new Ball[PERMAX];   // hand object
	Ball b[] = new Ball[BALL_NUM_MAX];  // ball object
	int tw;
	int aw;
	int ballNum;
	int max_height;

	int jPerNo;       // person's No for ball.juggling()

	MessageBox msg = new MessageBox(); // Class of MessageBox(use only in test mode)

	int patt[][] = new int[LMAX][MMAX];
	int patts[] = new int[LMAX];
	int pattw;
	int r[] = new int[LMAX*2];
	float high[] = new float[BALL_NUM_MAX+1];
	int patt_x;
	int kw0;
	String singleSS[] = new String[LMAX];

	AudioClip ac[] = new AudioClip[36];

	// hereafter were in ImageFrame

	Color color[] ={
		new Color(255, 255, 255),
		new Color( 80,  80,  80),
		new Color(  0,   0,   0),
		new Color(  0, 100, 200),
		new Color(200,   0, 100),
		new Color(100, 200,   0),
		new Color( 50, 150, 200),
		new Color(200,  50, 150),
		new Color(100, 200,  50),
		new Color(  0, 150,  50),
		new Color( 50,   0, 150),
		new Color(150,  50,   0),
		new Color(  0, 200,   0),
		new Color(255, 200,   0),
		new Color(255,   0,   0),
		new Color(  0,   0, 200)
	};

	public final  int PM_W = 32;
	public final  int PM_H = 24;
	final int NCOLOR = 16;
	final int IMAGE_WIDTH = 480;
	final int IMAGE_HEIGHT = 440;
	final int HOR_CENTER = (IMAGE_WIDTH/2);
	final int VER_CENTER = (IMAGE_HEIGHT/2);

	int HOR_MARGIN = 20;  // margin of right-left
	int VER_MARGIN = 20;  // margin of up-down

	Graphics gg1 = null;
	Graphics gg2 = null;

	Image bm[] = new Image[NCOLOR];
	Image r_bm[] = new Image[NCOLOR];
	Image l_bm[] = new Image[NCOLOR];

	Graphics bm_gc[] = new Graphics[NCOLOR];
	Graphics r_bm_gc[] = new Graphics[NCOLOR];
	Graphics l_bm_gc[] = new Graphics[NCOLOR];

	Image image_pixmap;
	Graphics image_gc;

	int bm1;
	int bm2;

	// above were in ImageFrame

	public void initialize(){
		String embed = "";
		try{
			embed = getParameter("embed");
		}
		catch(Throwable e){}
		if(embed.equalsIgnoreCase("true")){
			Y_OFFSET = 0;
			setBackground(Color.white);
			resize(IMAGE_WIDTH, IMAGE_HEIGHT + Y_OFFSET + 20);
			validate();
			setVisible(true);
			image_pixmap = createImage(IMAGE_WIDTH, IMAGE_HEIGHT + 20);
			image_gc = image_pixmap.getGraphics();
		}else{
			imf = new ImageFrame(this);
			Y_OFFSET = 20;
			imf.setLayout(null);
			imf.setBackground(Color.white);
			imf.setSize(IMAGE_WIDTH, IMAGE_HEIGHT + Y_OFFSET + 20);
			imf.validate();
			Dimension d = getToolkit().getScreenSize();
			imf.setLocation(d.width / 2, 0);
			d = null;
			imf.setVisible(true);
			image_pixmap = imf.createImage(IMAGE_WIDTH, IMAGE_HEIGHT + 20);
			image_gc = image_pixmap.getGraphics();
		}

		holder = new PatternHolder(this);

		//motionarray2[][] にノーマルのパターンを入れておく
		int icnt;
		for(icnt=0; icnt<PERMAX; icnt++){
			holder.getMotion2("Normal", icnt);
		}
		//MessageBox("motionlenght[0]:" + motionlength[0] + "\nmotionarray2[0][0]:" + motionarray2[0][0]);

		String noquit = "";
		try{
			getParameter("noquit");
		}
		catch(Throwable e){}
		controller = new JmjController(this, noquit);
		controller.setLocation(0, 0);
		controller.setVisible(true);
		controller.enableMenuBar();

		for( int i = 0; i < BALL_NUM_MAX; i++ ){
			b[i] = new Ball();
		}
		Ball.jmj = this;

		int i;
		for(i = 0; i < PERMAX; i++ ){
			ap[i] = new Arm();
			rhand[i] = new Ball();
			lhand[i] = new Ball();
		}

		// If no-data in pattern file, at first set dummy data.
		SetXYDummyData();

		readParameter();
		if(startindex == -1){
			startindex = fallback_startindex;
			startJuggling(startindex, null);
			controller.patternList.select(startindex);
		}

		// 音声出力用
		for(i = 0; i <= 9; i++){
			URL url = null;
			try{
				url=getDocumentBase();
			}
			catch(Throwable e){}
			ac[i] = getAudioClip(url,"./sound/" + i + ".wav");
			if(ac[i] == null){
				System.out.println("getAudioClip " + i + " failed!");
				break;
			}
		}
		for(i = 10; i <= 35; i++){
			char c = (char)((int)'a' + i - 10);
			URL url = null;
			try{
				url=getDocumentBase();
			}
			catch(Throwable e){}
			ac[i] = getAudioClip(url,"./sound/" + c + ".wav");
			if(ac[i] == null){
				System.out.println("getAudioClip " + c + " failed!");
				break;
			}
		}
	}
	public void quit(){
		stop();
		destroy();
	}
	public void destroy(){
		stopJuggling();
		try{
			controller.setVisible(false);
			controller.dispose();
		}
		catch(Throwable e){}
		try{
			disposeGraphics();
			if(imf != null){
				imf.dispose();
				imf.setVisible(false);
			}
		}
		catch(Throwable e){}
	}

	void stopJuggling(){
		kicker = null;
		status = State.IDLE;
	}
	void readParameter(){
		String file = "pattern.jm";
		try{
			file = getParameter("file");
		}
		catch(Throwable e){}
		try{
			startpattern = getParameter("startwith");
		}
		catch(Throwable e){}
		try{
			patternfiles = getParameter("patternfiles");
		}
		catch(Throwable e){}
		if(file.length() != 0){
			openFile(file);
		}
		if(startindex >= 0){
			synchronized(this){
				startJuggling(startindex);
			}
			return;
		}
		if(startpattern != null && startpattern.length() > 0){
			if(!startJuggling(SITESWAP_MODE, startpattern)){
				putError("Error in <param> tag with \'startwith\' term.",
						"Mail this message to the webmaster");
				return;
			}
		}
	}
	void openFile(String file){
		synchronized(holder){
			BufferedReader fp = null;
			controller.disableSwitches();
			stopJuggling();
			controller.disableMenuBar();
			controller.patternList.removeAll();
			System.gc();
			try{
				//fp = new BufferedReader(new FileReader(file));
				fp = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"));
			}
			catch(Throwable e){}
			finally{
				if(fp == null){
					putError("File not found: ", file);
					controller.enableMenuBar();
					return;
				}
			}
			controller.putPrimaryMessage("loading" + file);
			try{
				if(!holder.setHolder(fp))
					return;
			}
			finally{
				try{
					fp.close();
				}
				catch(IOException e){
				}
			}
			controller.putMessage("Done loading " + file,"preparing patterns table.wait...");
			filename = file;
			for(int i = 0; ; i++){
				String s = holder.nameAt(i);
				if(s.length() == 0){
					break;
				}
				controller.patternList.add(s);
			}
			controller.patternList.validate();
			controller.enableSwitches();
			controller.putMessage(new String(),new String());
			controller.enableMenuBar();
			controller.setSpeed(speed);
			controller.setPerno(iPerNo);
			controller.setIfMirror(mirror);
			controller.setIfShowSiteSwap(showSS);
			controller.setIfShowBody(hand_on);
			controller.setIfSound(bSound);
			controller.patternList.select(startindex);
			System.gc();
		}
	}

	void setDpm(){
		speed = 2;
		base = 0;
		dpm = 400;

		gy_max = -20000;
		gy_min = 20000;
		gx_max = -20000;
		gx_min = 20000;

		iMoveX = 0;  // revision for X-direction

		if(!pattInitialize()) return;

		set_xmin_xmax();

		if(gy_max-gy_min > 0){
			dpm = (int)((float)400 * 340 / (gy_max - gy_min));
			if(dpm > DW){
				dpm = DW;
			}

			float xdpm = (IMAGE_WIDTH - HOR_MARGIN * 2) / (float)(gx_max - gx_min);
			if(xdpm > 1) xdpm = 1;

			dpm = Math.min((int)(xdpm * DW), dpm);

			gx_min = gx_min * DW / 400;
			gx_max = gx_max * DW / 400;

			// determine area(iXmin to iXmax) on gg2
			iXmax = (gx_max - gx_min) * dpm / (2 * DW) + HOR_CENTER;
			iXmin = IMAGE_WIDTH - iXmax;

			iMoveX = HOR_CENTER - (gx_max + gx_min) / 2 * dpm /DW;
			base = (int)(370 - (float)gy_max * dpm / 400);
		}
	}

	/////////////////////////////////////////////////////////////////////
	//   set_xmin_xmax()
	//   set gx_min, gx_max, gy_min, gy_max
	/////////////////////////////////////////////////////////////////////
	void set_xmin_xmax(){
		for(timeCount = 0; timeCount < tw * (pattw + max_height + (motionarray.length / 4)); timeCount++){
			int i;
			for(i = 0; i < ballNum; i++){
				b[i].juggle();
				gy_max = Math.max(gy_max, b[i].gy);
				gy_min = Math.min(gy_min, b[i].gy);
				gx_max = Math.max(gx_max, b[i].gx);
				gx_min = Math.min(gx_min, b[i].gx);
			}

			for(jPerNo=0; jPerNo<iPerNo; jPerNo++){
				rhand[jPerNo].juggle();
				lhand[jPerNo].juggle();
				gy_max = Math.max(gy_max, rhand[jPerNo].gy);
				gy_min = Math.min(gy_min, rhand[jPerNo].gy);
				gy_max = Math.max(gy_max, lhand[jPerNo].gy);
				gy_min = Math.min(gy_min, lhand[jPerNo].gy);
				gx_max = Math.max(gx_max, rhand[jPerNo].gx);
				gx_min = Math.min(gx_min, rhand[jPerNo].gx);
				gx_max = Math.max(gx_max, lhand[jPerNo].gx);
				gx_min = Math.min(gx_min, lhand[jPerNo].gx);

				ap[jPerNo].rx[0] = rhand[jPerNo].gx + 11 + arm_x;
				ap[jPerNo].ry[0] = rhand[jPerNo].gy + 11 + arm_y;
				ap[jPerNo].lx[0] = lhand[jPerNo].gx + 11 - arm_x;
				ap[jPerNo].ly[0] = lhand[jPerNo].gy + 11 + arm_y;

				arm_line(jPerNo);

				for(i = 0; i < 5; i++){
					gx_max = Math.max(gx_max, ap[jPerNo].rx[i]);
					gx_max = Math.max(gx_max, ap[jPerNo].lx[i]);
					gx_min = Math.min(gx_min, ap[jPerNo].rx[i]);
					gx_min = Math.min(gx_min, ap[jPerNo].lx[i]);
					gy_max = Math.max(gy_max, ap[jPerNo].ry[i]);
					gy_max = Math.max(gy_max, ap[jPerNo].ly[i]);
					gy_min = Math.min(gy_min, ap[jPerNo].ry[i]);
					gy_min = Math.min(gy_min, ap[jPerNo].ly[i]);
				}
			}
		}
	}

	void arm_line(int j){
		int mx, my;
		int sx, sy;
		int iXhosei = 0, iYhosei = 0;

		if(mirror == false){
			iXhosei = iXData[j] * dpm / PXY;
		}else{
			iXhosei = -iXData[j] * dpm / PXY;
		}
		iYhosei = iYData[j] * dpm / PXY;

		sx = (int)((long)dpm * XR / kw0);
		sy = base - dpm / 3 - iYhosei;

		ap[j].rx[1] = (ap[j].rx[0] + (iXhosei + sx) * 2) / 3 + dpm / 12;
		ap[j].lx[1] = (ap[j].lx[0] + (iXhosei - sx) * 2) / 3 - dpm / 12;
		ap[j].ry[1] = (ap[j].ry[0] + sy) / 2 + dpm / 8;
		ap[j].ly[1] = (ap[j].ly[0] + sy) / 2 + dpm / 8;

		ap[j].rx[2] = (ap[j].rx[1] + (iXhosei + sx) * 3) / 4;
		ap[j].lx[2] = (ap[j].lx[1] + (iXhosei - sx) * 3) / 4;
		ap[j].ry[2] = (ap[j].ry[1] + sy * 2) / 3 - dpm / 25;
		ap[j].ly[2] = (ap[j].ly[1] + sy * 2) / 3 - dpm / 25;

		ap[j].rx[3] = (ap[j].rx[2] + (iXhosei + sx) * 2) / 3 - dpm / 13;
		ap[j].lx[3] = (ap[j].lx[2] + (iXhosei - sx) * 2) / 3 + dpm / 13;
		ap[j].ry[3] = (ap[j].ry[2] + sy * 2) / 3 - dpm / 40;
		ap[j].ly[3] = (ap[j].ly[2] + sy * 2) / 3 - dpm / 40;

		mx = (ap[j].rx[3] + ap[j].lx[3]) / 2;
		my = (ap[j].ry[3] + ap[j].ly[3]) / 2;

		ap[j].rx[4] = (mx * 2 + ap[j].rx[3]) / 3;
		ap[j].lx[4] = (mx * 2 + ap[j].lx[3]) / 3;
		ap[j].ry[4] = (my * 2 + ap[j].ry[3]) / 3;
		ap[j].ly[4] = (my * 2 + ap[j].ly[3]) / 3;

		ap[j].hx = mx;
		ap[j].hy = (my * 2 - dpm * 2 / 3 + base -iYhosei) / 3;
		ap[j].hr = dpm / 11;

		ap[j].rx[5] = ap[j].hx + dpm / 20;
		ap[j].lx[5] = ap[j].hx - dpm / 20;
		ap[j].ry[5] = ap[j].hy + dpm / 13;
		ap[j].ly[5] = ap[j].ry[5];
	}

	boolean pattInitialize(){
		// return false if wrong siteswap;
		float tw0, aw0;
		ballNum = 0;
		max_height = 0;

		int i, j, k;
		for(i = 0; i < pattw; i++){
			for(j = 0; j < patts[i]; j++){
				ballNum += Math.abs(patt[i][j]);
				max_height = Math.max(max_height, Math.abs(patt[i][j]));
			}
		}
		if(ballNum % pattw != 0){
			System.out.println("ballNum % pattw != 0");
			return false;
		}
		ballNum /= pattw;
		if(ballNum > BALL_NUM_MAX){
			System.out.println("Too many balls");
			return false;
		}
		for(i = 0; i < LMAX * 2; i++){
			r[i] = 0;
		}
		for(i = 0; i <= ballNum; i++){
			j = 0;
			while (r[j] == patts[j % pattw] && j < pattw + max_height){
				j++;
			}
			if(i == ballNum){
				if(j == pattw + max_height){
					break;
				}else{
					System.out.println("Mulitplex error");
					return false;
				}
			}
			b[i].st = 0;

			if(mirror){
				if((j + intsyn) % 2 != 0){
					b[i].thand = 1;
					b[i].chand = 1;
				}else{
					b[i].thand = 0;
					b[i].chand = 0;
				}
			}else{
				if((j + intsyn) % 2 != 0){
					b[i].thand = 0;
					b[i].chand = 0;
				}else{
					b[i].thand = 1;
					b[i].chand = 1;
				}
			}
			if(isSync){
				b[i].c = -(j / 2) * 2;
			}else{
				b[i].c = -j;
			}
			while (j < pattw + max_height){
				if(r[j] == patts[j % pattw]){
					return false;
				}else{
					r[j]++;
				}
				k = patt[j % pattw][patts[j % pattw] - r[j]];
				if(isSync && k < 0){
					if(j % 2 == 0){
						j += -k + 1;
					}else{
						j += -k - 1;
					}
				}else{
					j+=k;
				}
			}
		}

		if(max_height < 3){
			max_height = 3;
		}
		tw0 = (float)Math.sqrt(2 / ga * max_height * height) * 2 / (max_height - dwell * 2) * redrawRate / speed;
		tw = (int)fadd(tw0, 0);
		if(tw == 0){
			System.out.println("tw = 0");
			return false;
		}
		aw0 = tw0 * dwell * 2;
		aw = (int)fadd(aw0, 0);
		if(aw < 1){
			aw = 1;
		}
		if(aw > tw * 2 - 1){
			aw = tw * 2 - 1;
		}
		patt_x = HOR_CENTER / 8 - siteswap.length() / 2;

		kw0 = (int)((float)XR / KW);

		high[0] = -.2f * dpm;
		high[1] = (int)(ga * square(tw0 / redrawRate * speed) / 8 * dpm);
		for(i = 2; i <= max_height; i++){
			high[i] = (int)(ga * square((tw0 * i - aw0) / redrawRate * speed) / 8 * dpm);
		}

		for(i = 0; i < ballNum; i++){
			b[i].bh = 0;
			b[i].gx = 0;
			b[i].gy = VER_CENTER;
			b[i].gx0 = 0;
			b[i].gy0 = VER_CENTER;
			b[i].gx1 = 0;
			b[i].gy1 = VER_CENTER;
		}

		for(jPerNo = 0; jPerNo < iPerNo; jPerNo++){

			if(mirror){
				lhand[jPerNo].c = 0;
				if(isSync){
					rhand[jPerNo].c = 0;
				}else{
					rhand[jPerNo].c = -1;
				}
			}else{
				rhand[jPerNo].c = 0;
				if(isSync){
					lhand[jPerNo].c = 0;
				}else{
					lhand[jPerNo].c = -1;
				}
			}
			rhand[jPerNo].bh = 2;
			rhand[jPerNo].st = Ball.OBJECT_HAND;
			rhand[jPerNo].thand = 1;
			rhand[jPerNo].chand = 1;
			rhand[jPerNo].gx  = 0;
			rhand[jPerNo].gy  = VER_CENTER;
			rhand[jPerNo].gx0 = 0;
			rhand[jPerNo].gy0 = VER_CENTER;
			rhand[jPerNo].gx1 = 0;
			rhand[jPerNo].gy1 = VER_CENTER;

			lhand[jPerNo].bh = 2;
			lhand[jPerNo].st = Ball.OBJECT_HAND;
			lhand[jPerNo].thand = 0;
			lhand[jPerNo].chand = 0;
			lhand[jPerNo].gx  = 0;
			lhand[jPerNo].gy  = VER_CENTER;
			lhand[jPerNo].gx0 = 0;
			lhand[jPerNo].gy0 = VER_CENTER;
			lhand[jPerNo].gx1 = 0;
			lhand[jPerNo].gy1 = VER_CENTER;
		}

		for(i = 0; i < LMAX*2; i++){
			r[i] = 0;
		}
		return true;
	}

	final float square(float x){
		return x * x;
	}

	String getSingleSS(int i){
		char p[] = new char[256];
		int j,t;
		int index = 0;

		if(isSync){
			p[index++] = '(';
		}
		for(t = 0; t <= intsyn; t++){
			if(t != 0){
				p[index++] = ',';
			}
			if(patts[i] == 0){
				p[index++] = '0';
			}else{
				if(patts[i] > 1){
					p[index++] = '[';
				}
				for(j = 0; j < patts[i]; j++){
					if(Math.abs(patt[i][j]) < 10){
						p[index++] = (char)('0' + Math.abs(patt[i][j]));
					}else{
						p[index++] = (char)('a' + Math.abs(patt[i][j]) - 10);
					}
					if(patt[i][j] < 0){
						p[index++] = 'x';
					}
				}
				if(patts[i] > 1){
					p[index++] = ']';
				}
			}
			i++;
		}
		if(isSync){
			p[index++] = ')';
		}
		return new String(p, 0, index);
	}

	void patternPrint(int mode_){
		if(mode_ == 1){
			if(pattw > c0 && pattw > intsyn + 1){
				drawSiteswap(patt_x + tx, singleSS[c0], false);
				tx += singleSS[c0].length();
			}
			int c = time_period;
			if(c <= c0){
				tx = 0;
			}
			drawSiteswap(patt_x + tx, singleSS[c], true);
			c0 = c;
			return;
		}
		if(mode_ == 0){
			tx = 0;
			int i;
			for(i = 0; i < pattw; i += intsyn + 1){
				drawSiteswap(patt_x + tx, singleSS[i], false);
				tx += singleSS[i].length();
			}
			c0 = pattw;
		}
	}
	public void chooseTrickByName(String trickName){
		if(trickName == null || trickName.length() == 0){
			return;
		}
		if(holder != null){
			int i = holder.chooseTrickByName(trickName);
			if(i != -1){
				startJuggling(i, trickName);
			}
		}
	}

	void startJuggling(int index){
		startJuggling(index, null);
	}

	boolean startJuggling(int index, String s){
		int iCnt;
		stopJuggling();
		// 古いスレッドが完全に終了するのを待ちます。
		while(isActive){
			try{
				Thread.sleep(100);
			}
			catch(InterruptedException e){}
		}
		clearImage();

		speed = controller.getSpeed();
		mirror = controller.ifMirror();
		showSS = controller.ifShowSiteSwap();
		hand_on = controller.ifShowBody();
		bSound = controller.ifSound();
		iPerNo = controller.getPerNo();
		if(controller.isNewChoice() || index == SITESWAP_MODE){
			if(index == SITESWAP_MODE){
				holder.getPattern(s);
				height = controller.GetHeight_();
				dwell = controller.getDwell();
				//MessageBox("SITESWAP_MODE");
				for(iCnt = 0; iCnt<iPerNo; iCnt++){
					motion2[iCnt] = motion;
				}
			}else{
				//MessageBox("controller.isNewChoice()");
				if(index == -1 || !holder.isPattern(index)){
					return false;
				}
				holder.getPattern(index);

				String strs = "";
				int icnt;
				for(icnt = 0; icnt<10; icnt++){
					strs += "motion2["+icnt + "] = "+ motion2[icnt] +"\n";
				}
				strs += "Formation : " + formation + "\n";
				strs += "Pattern : " + pattern + "\n";
				strs += "Motion : " + motion + "\n";
				MessageBox(strs);

				// ここで motion2[]を技リストで選択された技のモーションに戻す
			}
			holder.getMotion(motion);
			for(iCnt = 0; iCnt < PERMAX; iCnt++){
				holder.getMotion2(motion2[iCnt], iCnt);
			}
			holder.getFormation(formation);

			// ここでformationから人数を割り出して、PerMax, iPerNo にセット
			iPerNo = iPerMax;

			controller.setPerno(iPerNo);

			if(isSync){
				intsyn = 1;
			}else{
				intsyn = 0;
			}
			siteswap = new String();
			int i;
			for(i = 0; i < pattw; i += intsyn +1){
				singleSS[i] = getSingleSS(i);
				siteswap = siteswap + singleSS[i];
			}
			setDpm();
			speed = controller.getSpeed();
			if(pattInitialize()){
				removeErrorMessage();
			}else{
				if(index != SITESWAP_MODE)
					holder.invalidate(index);
				putError("Wrong siteswap", pattern);
				return false;
			}
			controller.enableSwitches();
			controller.setHeight(height);
			controller.setDwell(dwell);
		}else{
			height = controller.GetHeight_();
			dwell = controller.getDwell();
			if(index == MOTION_MODE){
				//MessageBox("MOTION_MODE");

				motion = s;
				holder.getMotion(s);
				for(iCnt = 0; iCnt < PERMAX; iCnt++){
					holder.getMotion2(motion, iCnt);
				}

				holder.getFormation(formation);
				iPerNo = iPerMax;
				controller.setPerno(iPerNo);
				controller.setLabels();
			}else if(index == FORMATION_MODE && s != null){
				//MessageBox("FORMATION_MODE");
				motion = s;
				holder.getFormation(formation);
				for(iCnt = 0; iCnt<iPerMax; iCnt++){
					motion2[iCnt] = motion;
				}
				holder.getMotion(s);
				for(iCnt = 0; iCnt < PERMAX; iCnt++){
					holder.getMotion2(motion2[iCnt], iCnt);
				}
				iPerNo = iPerMax;
				controller.setPerno(iPerNo);
				controller.setLabels();
			}

			setDpm();
			speed = controller.getSpeed();
			pattInitialize();
		}

		if(showSS){
			patternPrint(0);
		}
		controller.setLabels();
		initBallGraphics();
		initGraphics();
		timeCount = 0;
		time_period = 0;

		status = State.JUGGLING;
		kicker = new Thread(this);
		kicker.start();
		return true;
	}
	public void run(){
		isActive = true;
		while(kicker != null){
			doJuggle();
			try{
				Thread.sleep((long)(1000 / redrawRate));
			}
			catch(InterruptedException e){}
		}
		isActive = false;
	}
	void doJuggle(){
		if(status != State.JUGGLING){
			return;
		}
		timeCount += 1;

		if(timeCount < aw){
			timeCount = aw;
		}
		time_period = (int)((timeCount - aw) / tw);
		time_period %= pattw;

		drawStatus();

		int i;
		for(i = 0; i < ballNum; i++){
			b[i].juggle();
		}

		int iCnt = 0;
		for(jPerNo =0; jPerNo < iPerNo; jPerNo++){
			iCnt += rhand[jPerNo].juggle() + lhand[jPerNo].juggle();
		}
		if(iCnt > 0){
			if(showSS){
				patternPrint(1);
			}
		}

		eraseBalls();

		for(int jPerNo = 0; jPerNo  < iPerNo; jPerNo ++){
			ap[jPerNo].rx[0] = rhand[jPerNo].gx + 11 + arm_x;
			ap[jPerNo].ry[0] = rhand[jPerNo].gy + 11 + arm_y;
			ap[jPerNo].lx[0] = lhand[jPerNo].gx + 11 - arm_x;
			ap[jPerNo].ly[0] = lhand[jPerNo].gy + 11 + arm_y;
			arm_line(jPerNo);
		}

		putBalls();
		if(imf != null){
			imf.repaint();
		}else{
			this.repaint();
		}
	}

	void removeErrorMessage(){
		controller.putMessage("", "");
	}
	void putError(String s1, String s2){
		controller.putMessage(s1, s2);
		System.err.println(s1);
		System.err.println(s2);
	}

	float fadd(float t, int x){
		return (float)(Math.floor(t * Math.pow(10, x) + .5f)/Math.pow(10, x));
	}

	// class ImageFrame extends Frame{

	public void update(Graphics g){
		paint(g);
	}
	public void paint(Graphics g){
		if(gg1 != null){
			gg1.drawImage(image_pixmap, 0, 0, null);
		}
		if(gg2 != null){
			gg2.drawImage(image_pixmap, 20 -iXmin, -20, null);
		}
	}
	void initGraphics(){
		Graphics g;
		if(imf != null){
			g = imf.getGraphics();
		}else{
			g = this.getGraphics();
		}
		if(gg1 != null){
			gg1.dispose();
		}
		gg1 = g.create(0, Y_OFFSET, IMAGE_WIDTH, 20);
		if(gg2 != null){
			gg2.dispose();
		}

		gg2 = g.create(iXmin - 20, 20 + Y_OFFSET,
				iXmax - iXmin + 41, IMAGE_HEIGHT - 20);
		g.dispose();
	}
	void disposeGraphics(){
		if(image_gc != null){
			image_gc.dispose();
		}
		try{
			for(int i = 0; i < NCOLOR; i++){
				bm_gc[i].dispose();
				r_bm_gc[i].dispose();
				l_bm_gc[i].dispose();
			}
		}
		catch(Throwable e){}
		if(gg1 != null){
			gg1.dispose();
		}
		if(gg2 != null){
			gg2.dispose();
		}
	}
	int toIndex(char c){
		if(c >= '0' && c <= '9'){
			return c - '0';
		}else if(c >= 'a' && c <= 'z'){
			return c - 'a' + 10;
		}else{
			return 0;
		}
	}
	void drawSiteswap(int x, String str, boolean is_red){
		if(image_gc == null) return;
		if(is_red){
			image_gc.setColor(Color.red);
			// 音声出力
			if(bSound){
				if(str.length() == 1){
					ac[toIndex(str.charAt(0))].play();
				}else{
					for(int i = 0; i<str.length(); i++){
						char c = str.charAt(i);
						if((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z' && c != 'x')){
							ac[toIndex(c)].play();
						}
					}
				}
			}
		}else{
			image_gc.setColor(Color.black);
		}
		image_gc.drawString(str, x * 8, 420);
	}

	void drawBall(Image bm, int x, int y){
		if(x < -iMoveX || x > IMAGE_WIDTH - iMoveX || y < 0 || y > IMAGE_HEIGHT - 24){
			return;
		}
		Graphics g = image_gc.create(fx(x + bm1),
				y + bm1,
				bm2 - bm1 + 1,
				bm2 - bm1 + 1);

		g.drawImage(bm, -bm1, -bm1, null);
		g.dispose();
	}
	void drawLine(int x1, int y1, int x2, int y2){
		image_gc.drawLine(fx(x1), y1, fx(x2), y2);
	}
	void drawCircle(int x, int y, int r){
		image_gc.drawOval(fx(x - r), y - r, 2 * r, 2 * r);
	}
	void fillBox(int x1_b, int y1, int x2_b, int y2){
		image_gc.fillRect(fx((x1_b - (x2_b - x1_b)) * 8), y1, (x2_b - x1_b + 1) * 8 * 2, y2 - y1 + 1);
	}
	void initBallGraphics(){
		int data[] = {
			0, 18, 0,23,17,23,20,22,
			22,20,23,17,23,12,18,12,
			18,16,16,18, 0,18,
			12,15,23,17
		};

		int i;
		if(bm[0] == null){
			if( imf != null){
				l_bm[1] = imf.createImage(PM_W, PM_H);
				r_bm[1] = imf.createImage(PM_W, PM_H);
			}else{
				l_bm[1] = this.createImage(PM_W, PM_H);
				r_bm[1] = this.createImage(PM_W, PM_H);
			}
			l_bm_gc[1] = l_bm[1].getGraphics();
			r_bm_gc[1] = r_bm[1].getGraphics();
			for(i = 0; i < NCOLOR; i++){
				if(imf != null){
					bm[i] = imf.createImage(PM_W, PM_H);
				}else{
					bm[i] = this.createImage(PM_W, PM_H);
				}
				bm_gc[i] = bm[i].getGraphics();
				l_bm[i] = l_bm[1];
				l_bm_gc[i] = l_bm_gc[1];
				r_bm[i] = r_bm[1];
				r_bm_gc[i] = r_bm_gc[1];
			}
		}
		for(i = 0; i < NCOLOR; i++){
			bm_gc[i].setColor(Color.white);
			bm_gc[i].fillRect(0, 0, PM_W, PM_H);
		}
		l_bm_gc[1].setColor(Color.white);
		l_bm_gc[1].fillRect(0, 0, PM_W, PM_H);
		r_bm_gc[1].setColor(Color.white);
		r_bm_gc[1].fillRect(0, 0, PM_W, PM_H);

		for(i = 0; i < data.length; i++){
			data[i] = (data[i] - 11) * dpm / DW;
		}
		hand_x = data[i - 4] + 2;
		hand_y = data[i - 3] + 2;
		arm_x = data[i - 2];
		arm_y = data[i - 1];
		for(i = 0; i+6 < data.length; i += 2){
			r_bm_gc[1].setColor(color[1]);
			r_bm_gc[1].drawLine(
					11 + data[i], 10 + data[i+1],
					11 + data[i + 2],10 + data[i + 3]);
			l_bm_gc[1].setColor(color[1]);
			l_bm_gc[1].drawLine(
					12 - data[i], 10 + data[i + 1],
					12 - data[i + 2],10 + data[i + 3]);
		}
		int r = 11 * dpm / DW;
		for(i = 0; i < NCOLOR; i++){
			bm_gc[i].setColor(color[i]);
			bm_gc[i].fillOval(
					11 - r, 11 - r, 2 * r, 2 * r);
		}
		bm1 = 11 - 11 * dpm / DW;
		bm2 = 11 + 11 * dpm / DW + 1;
	}
	void clearImage(){
		image_gc.setColor(color[0]);
		image_gc.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
		Graphics g;
		if(imf != null){
			g = imf.getGraphics();
		}else{
			g = this.getGraphics();
		}
		g.drawImage(image_pixmap, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, null);
		g.dispose();
	}
	void eraseBalls(){
		int i, j;
		image_gc.setColor(color[0]);

		if(hand_on){
			for(j = 0; j < iPerNo; j++){
				for(i = 0; i < 5; i++){
					// erase arm line
					drawLine(ap[j].rx[i], ap[j].ry[i], ap[j].rx[i + 1], ap[j].ry[i + 1]);
					drawLine(ap[j].lx[i], ap[j].ly[i], ap[j].lx[i + 1], ap[j].ly[i + 1]);
				}
				// erase face
				drawCircle(ap[j].hx, ap[j].hy, ap[j].hr);

				// erase hand
				fillBox(rhand[j].gx0 / 8, rhand[j].gy0 + bm1,
						rhand[j].gx0 / 8 + 3, rhand[j].gy0 + bm2);
				fillBox(lhand[j].gx0 / 8, lhand[j].gy0 + bm1,
						lhand[j].gx0 / 8 + 3, lhand[j].gy0 + bm2);
			}
		}

		// erase ball
		for(i = ballNum - 1; i >= 0; i--){
			fillBox(b[i].gx0 / 8, b[i].gy0 + bm1,
					b[i].gx0 / 8 + 3, b[i].gy0 + bm2);
		}
		return;
	}
	void putBalls(){
		int i, j;
		if(hand_on){
			image_gc.setColor(color[1]);

			for(j = 0; j < iPerNo; j++){
				// draw hands
				drawBall(r_bm[1], rhand[j].gx, rhand[j].gy);
				drawBall(l_bm[1], lhand[j].gx, lhand[j].gy);

				// draw arm line
				for(i = 0; i < 5; i++){
					drawLine(ap[j].rx[i], ap[j].ry[i], ap[j].rx[i + 1], ap[j].ry[i + 1]);
					drawLine(ap[j].lx[i], ap[j].ly[i], ap[j].lx[i + 1], ap[j].ly[i + 1]);
				}
				drawCircle(ap[j].hx, ap[j].hy, ap[j].hr);
			}
		}
		for(i = ballNum - 1; i >= 0; i--){
			drawBall(bm[15 - i % 13], b[i].gx, b[i].gy);
		}
	}

	/////////////////////////////////////////////////////////
	//   MessageBox(String str)
	//   When TEST_MODE == true, display str on dialog-box
	/////////////////////////////////////////////////////////
	void MessageBox(String str){
		if(TEST_MODE == false) return;
		msg.msgbx(str);
	}

	/////////////////////////////////////////////////////////
	//   SetXYDummyData()
	//   Set dummy data to iXData and iYData
	/////////////////////////////////////////////////////////
	void SetXYDummyData(){
		int iXDummy[] = {0,30, 60, 90, 120, 150, 180, 210, 240, 270};
		int iYDummy[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		int iCnt;
		for(iCnt = 0; iCnt < 10; iCnt++){
			iXData[iCnt] = iXDummy[iCnt];
			iYData[iCnt] = iYDummy[iCnt];
		}
		return;
	}

	int fx(int x){
		return x + iMoveX;
	}

	////////////////////////////////////////////////////////////
	//  04.12.16   T.Okada
	//  腕、ボールのステータス表示
	//  内部的に各種ステータスを表示させるための関数
	//  通常は、フラグで表示させないようにしておく
	//  元のソースに説明がないため、ステータスがどんな状態かわからないため作成
	//  ソースを改変したい方は、ここに必要な情報を入れてみてください。
	//  メイン画面に情報が表示されます。
	////////////////////////////////////////////////////////////
	void drawStatus(){
		int iSt[][] = new int[7][21]; //iSt[0][]:HAND, iSt[1][]:MOVE, iSt[2][]:MOVE2, iSt[3][]:UNDER  [4]:c
		int iCnt, iCnt2;
		String strTmp;
		String strSts[] = {"OBJECT_HAND ","OBJECT_MOVE ","OBJECT_MOVE2","OBJECT_UNDER",
			"b[0]tP,cP,c ", "b[1]tP,cP,c ", "b[2]tP,cP,c "};

		if(TEST_MODE == false) return;
		if(image_gc == null) return;
		image_gc.drawString("0ABCDEFGHIJKLMNOPQRSTUVWXYZ1ABCDEFGHIJKLMNOPQRSTUVWXYZ2ABCDEFGHIJKLMNOPQRSTUVWXYZ3ABCDEFGHIJKLMNOPQRSTUVWXYZ4ABCDEFGHIJKLMNOPQRSTUVWXYZ5ABCDEFGHIJKLMNOPQRSTUVWXYZ", 0, 20);

		for(iCnt = 0; iCnt < iPerNo; iCnt++){
			iSt[0][iCnt * 2]     = (lhand[iPerNo - iCnt - 1].st & Ball.OBJECT_HAND) / Ball.OBJECT_HAND;
			iSt[0][iCnt * 2 + 1] = (rhand[iPerNo - iCnt - 1].st & Ball.OBJECT_HAND) / Ball.OBJECT_HAND;

			iSt[1][iCnt * 2]     = (lhand[iPerNo - iCnt - 1].st & Ball.OBJECT_MOVE) / Ball.OBJECT_MOVE;
			iSt[1][iCnt * 2 + 1] = (rhand[iPerNo - iCnt - 1].st & Ball.OBJECT_MOVE) / Ball.OBJECT_MOVE;

			iSt[2][iCnt * 2]     = (lhand[iPerNo - iCnt - 1].st & Ball.OBJECT_MOVE2) / Ball.OBJECT_MOVE;
			iSt[2][iCnt * 2 + 1] = (rhand[iPerNo - iCnt - 1].st & Ball.OBJECT_MOVE2) / Ball.OBJECT_MOVE;

			iSt[3][iCnt * 2]     = (lhand[iPerNo - iCnt - 1].st & Ball.OBJECT_UNDER) / Ball.OBJECT_UNDER;
			iSt[3][iCnt * 2 + 1] = (rhand[iPerNo - iCnt - 1].st & Ball.OBJECT_UNDER) / Ball.OBJECT_UNDER;
		}


		iSt[4][0] = (b[0].st & Ball.OBJECT_MOVE) / Ball.OBJECT_MOVE;
		iSt[5][0] = (b[0].st & Ball.OBJECT_MOVE2) / Ball.OBJECT_MOVE2;
		iSt[6][0] = (b[0].st & Ball.OBJECT_UNDER) / Ball.OBJECT_UNDER;

		iSt[4][1] = (b[1].st & Ball.OBJECT_MOVE) / Ball.OBJECT_MOVE;
		iSt[5][1] = (b[1].st & Ball.OBJECT_MOVE2) / Ball.OBJECT_MOVE2;
		iSt[6][1] = (b[1].st & Ball.OBJECT_UNDER) / Ball.OBJECT_UNDER;

		iSt[4][2] = (b[2].st & Ball.OBJECT_MOVE) / Ball.OBJECT_MOVE;
		iSt[5][2] = (b[2].st & Ball.OBJECT_MOVE2) / Ball.OBJECT_MOVE2;
		iSt[6][2] = (b[2].st & Ball.OBJECT_UNDER) / Ball.OBJECT_UNDER;

		image_gc.setColor(Color.white);
		image_gc.fillRect(0, 0, 420,200);
		image_gc.setColor(Color.black);

		image_gc.drawString("0 1 2 ", 122, 20);
		for(iCnt = 0; iCnt < 7; iCnt++){
			if(iCnt > 3) image_gc.setColor(color[16-(iCnt - 3)]);
			image_gc.drawString(strSts[iCnt], 10, 30+iCnt*10);
		}
		for(iCnt = 0; iCnt < 4; iCnt++){
			for(iCnt2 = 0; iCnt2 < iPerNo * 2; iCnt2++){
				if(iSt[iCnt][iCnt2] == 1){
					image_gc.setColor(Color.red);
				}else{
					image_gc.setColor(Color.black);
				}
				strTmp = "" + iSt[iCnt][iCnt2];
				image_gc.drawString(strTmp, 122 + iCnt2*16, 30+iCnt*10);
			}
		}
		for(iCnt = 4; iCnt < 7; iCnt++){
			for(iCnt2 = 0; iCnt2 < 3; iCnt2++){
				if(iSt[iCnt][iCnt2] == 1){
					image_gc.setColor(Color.red);
				}else{
					image_gc.setColor(Color.black);
				}
				strTmp = "" + iSt[iCnt][iCnt2];
				image_gc.drawString(strTmp, 122 + iCnt2*16, 50+iCnt*10);
			}
		}
	}

	public class ImageFrame extends Frame{
		private static final long serialVersionUID = -8904800112621689298L;
		Jmj jmj;
		public ImageFrame(Jmj j){
			super("Juggle Master Java " + strVer);
			jmj = j;
		}
		final public void update(Graphics g){
			paint(g);
		}
		final public void paint(Graphics g){
			jmj.paint(g);
		}
	}

	// application version starts here
	public static void main(String[] args){
		String jarPath = System.getProperty("java.class.path");
		dirPath = jarPath.substring(0, jarPath.lastIndexOf(File.separator)+1);
		Jmj jmj = new Jmj();
		jmj.initialize();
		System.out.println("Juggle Master Java initialized!");
	}
}
