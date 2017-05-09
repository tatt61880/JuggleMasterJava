import java.io.*;
import java.util.*;

public class PatternHolder{
	private final byte CROSS = (byte)'x' - 'a' + 10;
	private final byte COMMA = -1;
	private final byte BRA = -2;
	private final byte KET = -3;
	private final byte PAR = -4;
	private final byte ENTHESIS = -5;

	private Hashtable<String, byte[]> motiontable = new Hashtable<String, byte[]>();

	private Vector<Piece> patternVector;
	Enumeration<String> en = motiontable.keys();

	// 05.01.16  T.Okada  for person's formation
	private Hashtable<String, int[]> xyformation = new Hashtable<String, int[]>();
	Enumeration<String> enXY = xyformation.keys();

	Jmj jmj;
	private int fline, tailindex;
	boolean readflag;
	float dwell, height;
	char cbuf[] = new char[256];
	byte bbuf[] = new byte[Jmj.LMAX];
	int ibuf[] = new int[256];
	byte pattbarr[];
	String motion = Jmj.NORMAL;
	String motion2[] = new String[Jmj.PERMAX];

	String formation = Jmj.FORMATION_BASIC;
	String s;
	int next;

	final int X_MIN = -30;
	final int X_MAX = 30;
	final int Y_MIN = -10;
	final int Y_MAX = 20;

	PatternHolder(Jmj j){
		this.jmj = j;
		motiontable.put(motion, new byte[]{13, 0, 4, 0});
		xyformation.put(formation, new int[]{0, 0});
	}

	public boolean setHolder(BufferedReader fp){
		readflag = true;
		dwell = 0.5f;
		patternVector = new Vector<Piece>();
		height = 0.2f;
		fline = 0;
		try{
			resetmotion2();
			boolean fallback_incrementflag = true;
			while(true){
				if(readflag){
					s = fp.readLine();
					fline++;
				}
				readflag = true;
				tailindex = getTail(s);
				if(tailindex == 0){
					resetmotion2();
					continue;
				}
				switch(s.charAt(0)){
					case '/':
						patternVector.addElement(new Piece(false, s.substring(1, ++tailindex)));
						if(fallback_incrementflag){
							jmj.fallback_startindex++;
						}
					case ';':
						continue;
					case '#':
						wasParam(fp);
						continue;
					case '%':
						wasMotion(fp);
						continue;
					case '!':
						setFormation(fp);
						continue;
					case '$':
						int iii = motionToken();
						if(iii != 0){
							String strtt = "error " + iii + " \nline " + fline;
							jmj.MessageBox(strtt);
						}
						continue;

					default:
						fallback_incrementflag = false;
						pattbarr = parsePattern(s);
						if(pattbarr != null){
							Piece p = new Piece(true, s.substring( next, ++tailindex), motion,
									pattbarr, height, dwell, formation, motion2);
							patternVector.addElement(p);

							if(p.name.equals(jmj.startpattern)){
								jmj.startindex = patternVector.size() - 1;
							}
						}else{
							jmj.putError("Bad Pattern Definition in line :"+fline, s);
						}
				}
			}
		}
		catch(IOException e){
		}
		catch(NullPointerException e){
		}
		return !patternVector.isEmpty();
	}

	void wasParam(BufferedReader fp){
		StringTokenizer st = new StringTokenizer(s, "= ;\t");
		float t;
		int i;
		try{
			st.nextToken();
			t = Float.valueOf(st.nextToken()).floatValue();
		}
		catch(NumberFormatException e){
			jmj.putError("Not Number in line:" + fline, s);
			return;
		}

		cbuf[0] = s.charAt(1);
		cbuf[1] = s.charAt(2);
		if(      cbuf[0] =='D' && cbuf[1] =='R' && t >= 0.10f && t <=  0.90f){
			dwell = fadd(t, 2);
		}else if(cbuf[0] =='H' && cbuf[1] =='R' && t >= 0.01f && t <=  1.00f){
			height = fadd(t, 2);
		}else if(cbuf[0] =='G' && cbuf[1] =='A' && t >  0.00f && t <= 98.00f){
			jmj.ga = t;
		}else if(cbuf[0] =='S' && cbuf[1] =='P' && t >= 0.10f && t <=  2.00f){
			jmj.speed = fadd(t, 1);
		}else{
			i = (int) t;
			if(      cbuf[0] =='M' && cbuf[1] =='R' && (i == 0 || i == 1)){
				jmj.mirror = (i == 1);
			}else if(cbuf[0] =='H' && cbuf[1] =='D' && (i == 0 || i == 1)){
				jmj.hand_on = (i == 1);
			}else if(cbuf[0] =='P' && cbuf[1] =='D' && (i == 0 || i == 1)){
				jmj.showSS = (i == 1);
			}else if(cbuf[0] =='B' && cbuf[1] =='P'){
			}else if(cbuf[0] =='B' && cbuf[1] =='C'){
			}else{
				jmj.putError("Invalid Parameter Value in line:" + fline, s);
			}
			return;
		}
	}
	void wasMotion(BufferedReader fp) throws IOException{
		String tmp = s.substring( 1, ++tailindex );
		s = fp.readLine();
		fline++;
		readflag = false;
		if(s.length() == 0  || s.charAt(0) != '{'){
			if(motiontable.containsKey(tmp)){
				motion = tmp;
			}else{
				jmj.putError("Undefined Pattern in line:" + fline, s);
			}
			return;
		}
		int bindex = 0;
		try{
			String s1;
			while(s.length() != 0 && s.charAt(0) == '{'){
				StringTokenizer st = new StringTokenizer(s, " {},\t");
				for(int i = 0; i < 4; i++, bindex++){
					try{
						s1 = st.nextToken();
					}
					catch (NoSuchElementException e){
						throw new NumberFormatException();
					}
					bbuf[bindex] = Byte.parseByte(s1);
					if( (bbuf[bindex] < Y_MIN || bbuf[bindex] > Y_MAX) &&
							((bindex & 1) == 1) &&
							(bbuf[bindex] < X_MIN || bbuf[bindex] > X_MAX)){
						throw new NumberFormatException();
							}
				}
				s = fp.readLine();
				fline++;
			}

			motion = tmp;
			byte b[] = new byte[bindex];
			for(int i = 0; i < bindex; i++){
				b[i] = bbuf[i];
			}
			motiontable.put(motion, b);
			}
		catch(NumberFormatException e){
			jmj.putError("Bad motion definition in " + tmp + " in line:" + fline, s);
			while(s.length() != 0 && s.charAt(0) == '{'){
				s = fp.readLine();
				fline++;
			}
			readflag = false;
			}
		}
	byte []parsePattern(String s){
		int bindex = 0;
		char c;
		for(next = 0; next <= tailindex; next++ ){
			if((c = s.charAt(next)) == ' ' || c == '\t'){
				break;
			}
		}
		for(int i = 0; i < next; i++, bindex++){
			switch((c = s.charAt(i))){
				case '(':
					bbuf[bindex] = PAR;
					break;
				case ')':
					bbuf[bindex] = ENTHESIS;
					break;
				case ',':
					bbuf[bindex] = COMMA;
					break;
				case '[':
					bbuf[bindex] = BRA;
					break;
				case ']':
					bbuf[bindex] = KET;
					break;
				default:
					if(c >= '0' && c <= '9'){
						bbuf[bindex] = (byte)(c - '0');
					}else if(c >= 'a' && c <= 'z'){
						bbuf[bindex] = (byte)(c - 'a' + 10);
					}else if(c >= 'A' && c <= 'Z'){
						bbuf[bindex] = (byte)(c - 'A' + 10);
					}else{
						return null;
					}
			}
		}
		if(next > Jmj.LMAX ){
			jmj.putError("Too Long Pattern in line :"+fline, s);
			return null;
		}
		byte patt[] = new byte[bindex];
		for(int i = 0; i < bindex; i++){
			patt[i] = bbuf[i];
		}
		for(; next < tailindex; next++){
			if(s.charAt(next) != ' ' && s.charAt(next) != '\t'){
				break;
			}
		}
		if(next >= tailindex){
			next = 0;
		}
		return patt;
	}

	boolean isPattern(int index){
		return patternVector.elementAt(index).isPattern;
	}
	boolean getPattern(int index){
		return getPattern(index, null);
	}
	boolean getPattern(String s){
		tailindex = getTail(s);
		return getPattern(-1, s);
	}
	boolean getPattern(int index, String s){
		int flag = 0, flag2 = 0, a = 0;
		int pattw = 0;

		if(s != null){
			pattbarr = parsePattern(s);
			if(pattbarr == null){
				return false;
			}
			jmj.pattern = s;
		}else{
			if(!isPattern(index)){
				return false;
			}
			Piece p = (Piece)patternVector.elementAt(index);
			jmj.pattern = p.name;
			jmj.motion = p.motion;
			jmj.height = p.height;
			jmj.dwell = p.dwell;
			pattbarr = p.siteswap;
			jmj.formation = p.formation;

			int iCnt;
			for(iCnt = 0; iCnt < Jmj.PERMAX; iCnt++){
				jmj.motion2[iCnt] = p.motion2[iCnt];
			}
		}

		jmj.isSync = (pattbarr[0] == PAR);
		int j = 0;
		while(j < pattbarr.length){
			if(pattbarr[j] == BRA){
				flag2 = 1;
				jmj.patts[pattw] = 0;
				j++;
				continue;
			}
			if(pattbarr[j] == KET){
				if(flag2 == 0){
					return false;
				}
				flag2 = 0;
				pattw++;
				j++;
				continue;
			}
			if(jmj.isSync){
				switch(pattbarr[j]){
					case PAR:
						if(flag != 0){
							return false;
						}
						flag = 1;
						j++;
						continue;
					case ENTHESIS:
						if(flag != 5){
							return false;
						}
						flag = 0;
						j++;
						continue;
					case COMMA:
						if(flag != 2){
							return false;
						}
						flag = 4;
						j++;
						continue;
					case CROSS:
						if(flag != 2 && flag != 5){
							return false;
						}
						if(flag2 != 0){
							jmj.patt[pattw][jmj.patts[pattw] - 1] = -a;
						}else{
							jmj.patt[pattw - 1][0] = -a;
						}
						j++;
						continue;
				}
			}
			a = pattbarr[j];
			if(jmj.isSync){
				if(a % 2 != 0){
					return false;
				}
				if(flag2 == 0 && flag != 1 && flag != 4){
					return false;
				}
				if(flag == 1){
					flag = 2;
				}
				if(flag == 4){
					flag = 5;
				}
			}
			if(flag2 != 0){
				if(a == 0){
					return false;
				}
				jmj.patt[pattw][jmj.patts[pattw]++] = a;
				if(jmj.patts[pattw] > Jmj.MMAX){
					return false;
				}
			}else{
				jmj.patt[pattw][0] = a;
				if(a == 0){
					jmj.patts[pattw++] = 0;
				}else{
					jmj.patts[pattw++] = 1;
				}
			}
			j++;
		}
		if(flag != 0 || flag2 != 0 || pattw == 0){
			return false;
		}
		jmj.pattw = pattw;

		if(jmj.TEST_MODE == true){
			MessageBox msg = new MessageBox();
			msg.pattw = pattw;
			msg.isSync = jmj.isSync;
			msg.s = s;
			for(int iCnt1=0; iCnt1<pattw; iCnt1++){
				msg.patts[iCnt1] = jmj.patts[iCnt1];
				for(int iCnt2=0; iCnt2<jmj.patts[iCnt1]; iCnt2++){
					msg.patt[iCnt1][iCnt2] = jmj.patt[iCnt1][iCnt2];
				}
			}
			msg.chkSSdlg();
		}
		return true;
	}
	String nameAt(int index){
		try{
			return patternVector.elementAt(index).name;
		}
		catch(ArrayIndexOutOfBoundsException e){
			return new String();
		}
	}
	int chooseTrickByName(String trickName){
		int n = patternVector.size();
		for(int i = 0; i < n; i++){
			if(patternVector.elementAt(i).isPattern){
				if(((Piece)patternVector.elementAt(i)).name.equals(trickName)){
					jmj.controller.patternList.select(i);
					return i;
				}
			}
		}
		return -1;
	}

	// FIXME
	void invalidate(int index){
		patternVector.setElementAt(new Piece(false, patternVector.elementAt(index).name), index);
	}
	int getTail(String str){
		if(str.length() == 0){
			return 0;
		}
		for(int i = str.length() - 1; i > -1; i--){
			if(str.charAt(i) != ' ' && str.charAt(i) != '\t'){
				return i;
			}
		}
		return 0;
	}
	float fadd(float t, int x){
		return (float)(Math.floor(t * Math.pow(10, x) + 0.5f) / Math.pow(10, x));
	}
	void rewindMotion(){
		en = motiontable.keys();
	}
	String nextMotion(){
		if(en.hasMoreElements()){
			return (String) en.nextElement();
		}else{
			return new String();
		}
	}
	void getMotion(String motion){
		jmj.motionarray = (byte [])motiontable.get(motion);
	}

	void getMotion2(String motion, int iPer){
		if(motiontable == null){
			System.out.println("motiontable == null\n");
		}
		jmj.motionarray2[iPer] = (byte [])motiontable.get(motion);
		jmj.motionlength[iPer] = jmj.motionarray2[iPer].length;
	}

	int countMotions(){
		return motiontable.size();
	}

	/////////////////////////////////////////////////////////////
	//   void setFormation(BufferedReader fp)
	//   Set person's formation
	////////////////////////////////////////////////////////////
	void setFormation(BufferedReader fp) throws IOException{
		String tmp = s.substring(1, ++tailindex);
		s = fp.readLine();
		fline++;
		readflag = false;
		if(s.length() == 0  || s.charAt(0) != '{'){
			if(xyformation.containsKey(tmp)){
				formation = tmp;
			}else{
				jmj.putError("Undefined Formation in line:" + fline, s);
			}
			return;
		}
		int bindex = 0;
		try{
			String s1;
			while(s.length() != 0 && s.charAt(0) == '{'){
				StringTokenizer st = new StringTokenizer(s, " {},\t");
				for(int i = 0; i < 2; i++, bindex++){
					try{
						s1 = st.nextToken();
					}
					catch(NoSuchElementException e){
						throw new NumberFormatException();
					}
					ibuf[bindex] = Integer.parseInt(s1);

					if( (ibuf[bindex] < -1000 || ibuf[bindex] > 1000) &&
							((bindex & 1) == 1) &&
							(ibuf[bindex] < -1000 || ibuf[bindex] > 1000)){
						throw new NumberFormatException();
							}
				}
				s = fp.readLine();
				fline++;
			}

			formation = tmp;
			int ib[] = new int[bindex];
			for(int i = 0; i < bindex; i++){
				ib[i] = ibuf[i];
			}
			xyformation.put(formation, ib);
			}
		catch(NumberFormatException e){
			jmj.putError("Bad formation definition in " + tmp + " in line:" + fline, s);
			while(s.length() != 0 && s.charAt(0) == '{'){
				s = fp.readLine();
				fline++;
			}
			readflag = false;
			}
		}

	void rewindFormation(){
		enXY = xyformation.keys();
	}
	String nextFormation(){
		if(enXY.hasMoreElements()){
			return (String) enXY.nextElement();
		}else{
			return new String();
		}
	}

	/////////////////////////////////////////////////////////
	//   void getFormation(String formation)
	//   set formation on jmj.formationarray;
	/////////////////////////////////////////////////////////
	void getFormation(String formation){
		jmj.formationarray = (int[])xyformation.get(formation);
		Jmj.iPerMax = jmj.formationarray.length / 2;

		int iCnt;
		for(iCnt = 0; iCnt < Jmj.iPerMax; iCnt++){
			jmj.iXData[iCnt] = jmj.formationarray [iCnt * 2];
			jmj.iYData[iCnt] = jmj.formationarray [iCnt * 2 + 1];
		}
		return;
	}

	int countFormation(){
		return xyformation.size();
	}

	/////////////////////////////////////////////////////////
	//   int motionToken(String motion)
	//   set motion2[]
	//   1行をトークンに分けて、トークンの数字に対応する人のモーションを決定
	/////////////////////////////////////////////////////////
	int motionToken(){
		String strTmp;
		int iCnt, iCnt2;
		int iFlag[] = new int[Jmj.PERMAX];
		int iCntToken, iCntToken2;
		int iBefore, iAfter;
		String strToken1, strToken2, strToken3, strMotion;

		String stmp = s.substring(1);
		StringTokenizer st = new StringTokenizer(stmp, ",:");

		try{
			for(iCnt = 0; iCnt < Jmj.PERMAX; iCnt++){
				iFlag[iCnt] = 0;
			}

			iCntToken = st.countTokens();

			if(iCntToken < 2){ // トークンの数が1個だけ→モーション名がない場合
				return -1;
			}

			for(iCnt = 0; iCnt<iCntToken-1; iCnt++){
				strTmp = st.nextToken();
				StringTokenizer st2 = new StringTokenizer(strTmp, "-", true);
				iCntToken2 = st2.countTokens();
				// 1つのトークン中に'-'が2個以上あればエラー
				if(iCntToken2 > 3) return -2;

				switch(iCntToken2){
					case 1:
						//  トークンが数字かチェック
						//  iBefore, iAfter にトークンをセット
						iBefore = Integer.valueOf(st2.nextToken()).intValue();
						if(iBefore < 1 || Jmj.PERMAX < iBefore) return -3;
						iFlag[iBefore-1] = 1;
						break;
					case 2:
						//  どちらのトークンが"-"かチェック
						//  iBefore, iAfter にトークンをセット
						strToken1 = st2.nextToken();
						strToken2 = st2.nextToken();
						if(strToken1.equals("-")){
							iAfter = Integer.valueOf(strToken2).intValue();
							if(iAfter < 1 || Jmj.PERMAX < iAfter) return -4;
							iBefore = 1;
							for(iCnt2 = iBefore; iCnt2<iAfter+1; iCnt2++) iFlag[iCnt2-1] = 1;

						}else if(strToken2.equals("-")){
							iBefore = Integer.valueOf(strToken1).intValue();
							if(iBefore < 1 || Jmj.PERMAX < iBefore) return -5;
							iAfter = Jmj.PERMAX;
							for(iCnt2 = iBefore; iCnt2<iAfter+1; iCnt2++) iFlag[iCnt2-1] = 1;
						}else{
							return -123;
						}
						break;
					case 3:
						//  真中のトークンが"-"、前後が数値かをチェック
						//  iBefore, iAfter にトークンをセット
						strToken1 = st2.nextToken();
						strToken2 = st2.nextToken();
						strToken3 = st2.nextToken();
						if(strToken2.equals("-")){
							iBefore = Integer.valueOf(strToken1).intValue();
							if(iBefore < 1 || Jmj.PERMAX < iBefore) return -6;
							iAfter = Integer.valueOf(strToken3).intValue();
							if(iAfter < 1 || Jmj.PERMAX < iAfter) return -7;

							if(iBefore > iAfter){
								int Tmp = iAfter;
								iAfter = iBefore;
								iBefore = Tmp;
							}
							for(iCnt2 = iBefore; iCnt2<iAfter+1; iCnt2++){
								iFlag[iCnt2-1] = 1;
							}
						}else{
							return -124;
						}
						break;

					default:
						return -8;
				}
			}
			// 最後のトークンがモーション名かをチェック
			// すでに定義されたモーション名かもチェック

			strMotion = st.nextToken().substring(1);

			if(motiontable.containsKey (strMotion)){
				for(iCnt2 = 0; iCnt2<Jmj.PERMAX; iCnt2++){
					if(iFlag[iCnt2] == 1){
						motion2[iCnt2] = strMotion;
					}
				}
			}else{
				return -9;
			}
			return 0;
		}
		catch(NumberFormatException e){
			jmj.putError("Motion Error in line:" + fline, s);
			return -10;
		}

	}

	/////////////////////////////////////////////////////////
	//   void resetmotion2()
	//   reset motion2[]
	//   1行をトークンに分けて、トークンの数字に対応する人のモーションを決定
	/////////////////////////////////////////////////////////
	void resetmotion2(){
		int iCnt;
		for(iCnt = 0; iCnt < Jmj.PERMAX; iCnt++){
			motion2[iCnt] = "";
		}
		return;
	}

	static public class Piece{
		public String name;
		public boolean isPattern = false;
		String motion;
		byte siteswap[];
		float height;
		float dwell;
		String formation;
		String motion2[] = new String[Jmj.PERMAX];

		Piece(){}
		Piece(boolean isPat, String nm){
			name = nm;
			isPattern = isPat;
		}
		Piece(boolean isPat, String nm, String mt, byte []ss, float hght, float ht, String fm, String mt2[]){
			name = nm;
			motion = mt;
			siteswap = ss;
			height = hght;
			dwell = ht;
			formation = fm;
			isPattern = isPat;
			for(int iCnt = 0; iCnt< Jmj.PERMAX; iCnt++){
				if(mt2[iCnt] == ""){
					motion2[iCnt] = motion;
				}else{
					motion2[iCnt] = mt2[iCnt];
				}
			}
		}
	}
}
