import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.List;

public class JmjController extends Frame implements AdjustmentListener, ActionListener{
	private static final long serialVersionUID = -4281429447311487691L;
	private static final String QUIT = "Quit";
	private static final String TRY_A_NEW_SITESWAP = "Try a new siteswap";

	Jmj jmj;
	JmjDialog jd;

	Button new_siteswap_button;
	Button juggle_button;
	Button pause_button;
	Button showerize_button;

	Label primarymessage;
	Label secondarymessage;
	Label pattern_label;
	Label pattern_value;
	Label motion_label;
	Label motion_value;
	Label ballno_label;
	Label ballno_value;
	Label formation_label;
	Label formation_value;

	Label speed_label;
	Label speed_value;
	Label height_label;
	Label height_value;
	Label dwell_label;
	Label dwell_value;
	Label perno_label;
	Label perno_value;
	Scrollbar speed_gauge;
	Scrollbar height_gauge;
	Scrollbar dwell_gauge;
	Scrollbar perno_gauge;

	MenuBar menubar;
	Menu menu_option;
	Menu menu_quit;
	Menu menu_help;

	TextField dialog_text = new TextField();

	PatternList patternList;
	PatternFileList dialog_fileList = new PatternFileList();
	MotionList dialog_motionList = new MotionList();
	DidyoumeanList dialog_didyoumeanList = new DidyoumeanList();

	Button dialog_cancel = new Button("cancel");
	Button dialog_ok = new Button("OK");

	Checkbox mirror_box;
	Checkbox ss_box;
	Checkbox body_box;
	Checkbox sound_box;
	int prevIndex;

	static int iPersonNo;   // == jmj.iPerNo;

	class PatternList extends List{
		private static final long serialVersionUID = -2118886900172535681L;
		void chooseValidIndex(){
			int i;
			for(i = getSelectedIndex(); i < getItemCount(); i++){
				if(jmj.holder.isPattern(i)){
					select(i);
					return;
				}
			}
			for(i = getSelectedIndex(); i > -1; i--){
				if(jmj.holder.isPattern(i)){
					select(i);
					return;
				}
			}
			deselect(getSelectedIndex());
		}
	}
	class PatternFileList extends List{
		private static final long serialVersionUID = -8123075254992794852L;
		void create(){
			removeAll();
			if(jmj.patternfiles != null){
				String[] files = jmj.patternfiles.split("|");
				for(String file : files){
					add(file);
				}
			}
		}
	}
	static class ListWithQuicksort extends List{
		private static final long serialVersionUID = 8218993129332150086L;
		protected void quickSort(int left, int right){
			int i, last;
			if(left >= right) return;
			swap(left, (left + right)/2);
			last = left;
			for(i = left + 1; i <= right; i++){
				if(getItem(i).compareTo(getItem(left)) < 0){
					swap(++last, i);
				}
			}
			swap(last,left);
			quickSort(left, last - 1);
			quickSort(last + 1, right);
		}
		private void swap(int d1, int d2){
			String tmp = getItem(d1);
			replaceItem(getItem(d2), d1);
			replaceItem(tmp, d2);
		}
	}

	class MotionList extends ListWithQuicksort{
		private static final long serialVersionUID = 8889101774998964828L;
		void create(){
			if(jmj.holder.countMotions() > getItemCount()){
				String s;
				removeAll();
				jmj.holder.rewindMotion();
				while(true){
					s =jmj.holder.nextMotion();
					if(s.length() == 0) break;
					add(s);
				}
				quickSort(0, getItemCount() - 1);
			}
		}
	}
	class DidyoumeanList extends List{
		private static final long serialVersionUID = -433897089419292807L;
		// "Did you mean..." feature (「もしかして」機能)
		boolean create(String inputStr){
			removeAll();
			// TODO: Dealing Mulitplex and/or Sync patterns
			if(vanilla_siteswap_check(inputStr)){ // vanilla siteswap patterns
				Set<String> strList = new HashSet<String>();
				int[] pat = new int[Jmj.LMAX];

				int i, j;
				int sum = 0;
				jmj.holder.getPattern(inputStr);
				for(i = 0; i < jmj.pattw; i++){
					sum += jmj.patt[i][0];
					pat[i] = jmj.patt[i][0];
				}
				int average = sum / jmj.pattw;

				{ // Remove one elemenet
					// e.g. 551 => 55, 51
					for(i = 0; i < jmj.pattw; i++){
						int[] trial_pat = new int[Jmj.LMAX];
						System.arraycopy(pat, 0, trial_pat, 0, i);
						System.arraycopy(pat, i+1, trial_pat, i, jmj.pattw-i-1);
						strList.add(pat2string(trial_pat, jmj.pattw-1));
					}
				}
				if(sum % jmj.pattw == 0){
					{ // Swap two element
						// e.g. 7351 => 7531, 1357
						for(i = 0; i < jmj.pattw; i++){
							for(j = i+1; j < jmj.pattw; j++){
								int[] trial_pat = pat.clone();
								trial_pat[i] = pat[j];
								trial_pat[j] = pat[i];
								strList.add(pat2string(trial_pat, jmj.pattw));
							}
						}
					}
					{ // Moved one elemenet
						// e.g. 5371 => 7531
						for(i = 0; i < jmj.pattw; i++){ // move element index-i to index-j
							for(j = 0; j < jmj.pattw-1; j++){
								if(j < i-1){
									int[] trial_pat = new int[Jmj.LMAX];
									System.arraycopy(pat,   0, trial_pat,   0, j);
									trial_pat[j] = pat[i];
									System.arraycopy(pat,   j, trial_pat, j+1, i-j);
									System.arraycopy(pat, i+1, trial_pat, i+1, jmj.pattw-1-i);
									strList.add(pat2string(trial_pat, jmj.pattw));
								}else if(j > i+1){
									int[] trial_pat = new int[Jmj.LMAX];
									System.arraycopy(pat,   0, trial_pat,   0, i);
									System.arraycopy(pat, i+1, trial_pat,   i, j-i);
									trial_pat[j] = pat[i];
									System.arraycopy(pat, j+1, trial_pat, j+1, jmj.pattw-j);
									strList.add(pat2string(trial_pat, jmj.pattw));
								}
							}
						}
					}
				}else{
					{ // Change one element
						int diff1 = (sum % jmj.pattw);             // (large -> small) e.g. 551 => 531
						int diff2 = jmj.pattw - (sum % jmj.pattw); // (small -> large) e.g. 551 => 561, 552
						for(i = 0; i < jmj.pattw; i++){
							int[] trial_pat = pat.clone();
							trial_pat[i] = pat[i] - diff1;
							strList.add(pat2string(trial_pat, jmj.pattw));
							trial_pat[i] = pat[i] + diff2;
							strList.add(pat2string(trial_pat, jmj.pattw));
						}
					}
					{ // Add one element (>average)
						// e.g. 551 => 5551
						int valid_sum = (average+1) * (jmj.pattw + 1);
						int insert_value = valid_sum - sum;
						for(i = 1; i <= jmj.pattw; i++){
							int[] trial_pat = new int[Jmj.LMAX];
							System.arraycopy(pat, 0, trial_pat, 0, i);
							System.arraycopy(pat, i, trial_pat, i+1, jmj.pattw-i);
							trial_pat[i] = insert_value;
							strList.add(pat2string(trial_pat, jmj.pattw+1));
						}
					}
				}
				{ // Add one element (element <= average)
					// e.g. 551 => 5151, 5511 (1 < average)
					// e.g. 135 => 1353 (3 == average)
					int valid_sum = average * (jmj.pattw + 1);
					int insert_value = valid_sum - sum;
					for(i = 1; i <= jmj.pattw; i++){
						int[] trial_pat = new int[Jmj.LMAX];
						System.arraycopy(pat, 0, trial_pat, 0, i);
						System.arraycopy(pat, i, trial_pat, i+1, jmj.pattw-i);
						trial_pat[i] = insert_value;
						strList.add(pat2string(trial_pat, jmj.pattw+1));
					}
				}

				for(String str: strList){
					if(str.equals("")){
						continue;
					}
					jmj.holder.getPattern(str);
					if(jmj.pattInitialize()){
						add(str);
					}
				}
				if(getItemCount() > 0){
					return true;
				}else{
					return false;
				}
			}else{
				jmj.putError("[Did you mean...] is for vanilla-siteswap, now.", "It cannot handle this: " + jmj.siteswap);
				return false;
			}
		}
		private String pat2string(int[] pat, int length){
			String ret = "";
			for(int i = 0; i < length; i++){
				if(0 <= pat[i] && pat[i] <= 9){
					ret += (char)('0' + pat[i]);
				}else if(10 <= pat[i] && pat[i] <= 35){
					ret += (char)('a' + pat[i] - 10);
				}else{
					return "";
				}
			}
			return ret;
		}
	}

	public JmjController(Jmj jmj, String quitflag){
		super("Juggle Master Java " + jmj.strVer);
		this.jmj = jmj;

		setSize(480, 470);
		setLayout(null);
		setBackground(Color.white);
		setResizable(false);

		patternList = new PatternList();
		patternList.setBounds(270, 190, 200, 270);
		add(patternList);

		primarymessage = new Label();
		secondarymessage = new Label();
		primarymessage.setBounds(10, 400, 460, 20);
		secondarymessage.setBounds(10, 420, 460, 20);
		add(primarymessage);
		add(secondarymessage);

		new_siteswap_button = new Button(TRY_A_NEW_SITESWAP);
		juggle_button       = new Button("Juggle");
		pause_button        = new Button("Pause / Resume");
		showerize_button    = new Button("Showerize");
		new_siteswap_button.setBounds(280, 100, 150, 30);
		juggle_button.setBounds(50, 150, 100, 30);
		pause_button.setBounds(170, 150, 120, 30);
		showerize_button.setBounds(330, 150, 100, 30);
		add(new_siteswap_button);
		add(juggle_button);
		add(pause_button);
		add(showerize_button);

		pattern_label   = new Label("Pattern");
		motion_label    = new Label("Arm motion");
		ballno_label    = new Label("Ball #");
		formation_label = new Label("Formation");
		pattern_value = new Label("");
		motion_value = new Label("");
		ballno_value = new Label("");
		formation_value = new Label("");
		pattern_label.setBounds(10, 70, 100, 20);
		pattern_value.setBounds(120, 70, 300, 20);
		motion_label.setBounds(10, 90, 100, 20);
		motion_value.setBounds(120, 90, 300, 20);
		ballno_label.setBounds(10, 110, 100, 20);
		ballno_value.setBounds(120, 110, 100, 20);
		formation_label.setBounds(10, 130, 100, 20);
		formation_value.setBounds(120, 130, 300, 20);
		add(pattern_label);
		add(pattern_value);
		add(motion_label);
		add(motion_value);
		add(ballno_label);
		add(ballno_value);
		add(formation_label);
		add(formation_value);

		speed_label  = new Label("Speed");
		height_label = new Label("Height");
		dwell_label  = new Label("Dwell ratio");
		perno_label  = new Label("Person #");
		speed_label.setBounds( 10, 190, 100, 20);
		height_label.setBounds(10, 210, 100, 20);
		dwell_label.setBounds( 10, 230, 100, 20);
		perno_label.setBounds( 10, 250, 100, 20);
		speed_gauge  = new Scrollbar(Scrollbar.HORIZONTAL, 10,  3,  1,  20 +  3);
		height_gauge = new Scrollbar(Scrollbar.HORIZONTAL, 20, 15,  1, 100 + 15);
		dwell_gauge  = new Scrollbar(Scrollbar.HORIZONTAL, 20, 10, 10,  90 + 10);
		perno_gauge  = new Scrollbar(Scrollbar.HORIZONTAL, iPersonNo, 1, Jmj.PERMIN, Jmj.PERMAX + 1);
		speed_gauge.setBounds( 110, 190, 100, 20);
		height_gauge.setBounds(110, 210, 100, 20);
		dwell_gauge.setBounds( 110, 230, 100, 20);
		perno_gauge.setBounds( 110, 250, 100, 20);
		speed_value  = new Label("");
		height_value = new Label("");
		dwell_value  = new Label("");
		perno_value  = new Label("");
		speed_value.setBounds( 220, 190, 40, 20);
		height_value.setBounds(220, 210, 40, 20);
		dwell_value.setBounds( 220, 230, 40, 20);
		perno_value.setBounds( 220, 250, 40, 20);

		add(speed_label);
		add(height_label);
		add(dwell_label);
		add(perno_label);
		add(speed_gauge);
		add(height_gauge);
		add(dwell_gauge);
		add(perno_gauge);
		add(speed_value);
		add(height_value);
		add(dwell_value);
		add(perno_value);
		speed_gauge.addAdjustmentListener(this);
		height_gauge.addAdjustmentListener(this);
		dwell_gauge.addAdjustmentListener(this);
		perno_gauge.addAdjustmentListener(this);

		mirror_box = new Checkbox("Mirror image");
		mirror_box.setBounds(50, 300, 150, 20);
		add(mirror_box);

		ss_box = new Checkbox("Show siteswap");
		ss_box.setBounds(50, 320, 180, 20);
		add(ss_box);

		body_box = new Checkbox("Show juggler");
		body_box.setBounds(50, 340, 180, 20);
		add(body_box);

		sound_box = new Checkbox("Sound");
		sound_box.setBounds(50, 360, 180, 20);
		add(sound_box);

		menubar = new MenuBar();
		setMenuBar(menubar);

		menu_quit = new Menu(QUIT);
		menu_quit.add(new MenuItem(QUIT));

		if(!(quitflag != null && quitflag.equalsIgnoreCase("true"))){
			menubar.add(menu_quit);
		}
		menu_option = new Menu("Option");
		menu_option.add(new MenuItem(JmjDialog.STRING_LOAD));
		menu_option.add(new MenuItem(JmjDialog.STRING_SELECT_FILE));
		menu_option.add(new MenuItem(JmjDialog.STRING_SITESWAP));
		menu_option.add(new MenuItem(JmjDialog.STRING_MOTION));
		menu_option.add(new MenuItem(JmjDialog.STRING_FORMATION));
		menubar.add(menu_option);

		menu_help = new Menu("Help");
		menu_help.add(new MenuItem(JmjDialog.STRING_ABOUT));
		menubar.add(menu_help);

		menu_quit.addActionListener(this);
		menu_option.addActionListener(this);
		menu_help.addActionListener(this);
		new_siteswap_button.addActionListener(this);
		juggle_button.addActionListener(this);
		pause_button.addActionListener(this);
		showerize_button.addActionListener(this);
		patternList.addActionListener(this);

		dialog_text.setFont(new Font("Dialog", Font.PLAIN, 15));
		dialog_cancel.setSize(70, 20);
		dialog_ok.setSize(70, 20);
		dialog_motionList.setSize(189, 190);
		dialog_didyoumeanList.setSize(189, 190);

		validate();
	}

	public void adjustmentValueChanged(AdjustmentEvent e){
		Object target = e.getSource();
		if(target == speed_gauge){
			setSpeedLabel();
			return;
		}
		if(target == height_gauge){
			setHeightLabel();
			return;
		}
		if(target == dwell_gauge){
			setDwellLabel();
			return;
		}
		if(target == perno_gauge){
			iPersonNo = getPerNo();
			setPernoLabel();
			return;
		}
		return;
	}
	// No multiplex && no sync pattern => true
	private static boolean vanilla_siteswap_check(String str){
		for(int i = 0; i < str.length(); i++){
			char c = str.charAt(i);
			if('0' <= c && c <= '9'){
			}else if('a' <= c && c <= 'z'){
			}else{
				return false;
			}
		}
		return true;
	}

	public void actionPerformed(ActionEvent e){
		Object target = e.getSource();
		if(target == patternList){
			patternList.chooseValidIndex();
			juggle_pressed();
			return;
		}
		if(target == juggle_button){
			juggle_pressed();
			return;
		}
		if(target == pause_button){
			if(jmj.status == Jmj.State.PAUSE){
				jmj.status = Jmj.State.JUGGLING;
			}else if(jmj.status == Jmj.State.JUGGLING){
				jmj.status = Jmj.State.PAUSE;
			}
			return;
		}
		if(target == new_siteswap_button){
			jd = new JmjDialog(this);
			jd.popup(JmjDialog.TRY_SITESWAP);
		}
		if(target == showerize_button){ //なんでもシャワー
			if(vanilla_siteswap_check(jmj.siteswap)){ // vanilla siteswap patterns
				String newPatternStr = "";
				for(int i = 0; i < jmj.pattw; i++){
					int d = 2 * jmj.patt[i][0] - 1;
					if(jmj.patt[i][0] == 0){
						newPatternStr += "00";
					}else{
						newPatternStr += "1";
						if(d < 10){
							newPatternStr += String.valueOf(d);
						}else if(d < 36){
							newPatternStr += String.valueOf((char)('a'+d-10));
						}else{
							return;
						}
					}
				}
				jmj.startJuggling(Jmj.SITESWAP_MODE, newPatternStr);
				return;
			}else{
				jmj.putError("[Showerize] is only for vanilla-siteswap, now.", "It cannot handle this: " + jmj.siteswap);
				return;
			}
		}
		if(target instanceof MenuItem){
			if(e.getActionCommand().equals(QUIT)){
				jmj.quit();
			}
			if(e.getActionCommand().equals(JmjDialog.STRING_LOAD)){
				jd = new JmjDialog(this);
				jd.popup(JmjDialog.LOAD_FILE);
			}
			if(e.getActionCommand().equals(JmjDialog.STRING_SELECT_FILE)){
				jd = new JmjDialog(this);
				jd.popup(JmjDialog.SELECT_FILE);
			}
			if(e.getActionCommand().equals(JmjDialog.STRING_SITESWAP)){
				jd = new JmjDialog(this);
				jd.popup(JmjDialog.TRY_SITESWAP);
			}
			if(e.getActionCommand().equals(JmjDialog.STRING_MOTION)){
				try{
					jd = new JmjDialog(this);
					jd.popup(JmjDialog.CHOOSE_MOTION);
				}
				catch(ArrayIndexOutOfBoundsException aioobe){}
			}
			if(e.getActionCommand().equals(JmjDialog.STRING_FORMATION)){
				try{
					jd = new JmjDialog(this);
					jd.popup(JmjDialog.CHOOSE_FORMATION);
				}
				catch(ArrayIndexOutOfBoundsException aioobe){}
			}
			if(e.getActionCommand().equals(JmjDialog.STRING_ABOUT)){
				try{
					jd = new JmjDialog(this);
					jd.popup(JmjDialog.CHOOSE_ABOUT);
				}
				catch(ArrayIndexOutOfBoundsException aioobe){}
			}
			return;
		}
	}
	void juggle_pressed(){
		synchronized(jmj){
			jmj.startJuggling(patternList.getSelectedIndex());
		}
	}
	void setSpeedLabel(){
		speed_value.setText(String.valueOf(getSpeed()));
	}
	void setHeightLabel(){
		height_value.setText(String.valueOf(GetHeight_()));
	}
	void setDwellLabel(){
		dwell_value.setText(String.valueOf(getDwell()));
	}
	void setPernoLabel(){
		perno_value.setText(String.valueOf(iPersonNo));
	}

	void setLabels(){
		pattern_value.setText(jmj.pattern);
		ballno_value.setText(String.valueOf(jmj.ballNum));
		formation_value.setText(jmj.formation);
		motion_value.setText(jmj.motion);
	}
	void setSpeed(float speed){
		speed_gauge.setValue((int)(speed * 10));
		setSpeedLabel();
	}
	void setHeight(float height){
		height_gauge.setValue((int)(height * 100));
		setHeightLabel();
	}
	void setDwell(float dwell){
		dwell_gauge.setValue((int)(dwell * 100));
		setDwellLabel();
	}
	void setPerno(int i){
		iPersonNo = i;
		perno_gauge.setValues(iPersonNo, 1, Jmj.PERMIN, Jmj.iPerMax + 1);
		setPernoLabel();
	}

	float getSpeed(){
		return speed_gauge.getValue() / 10f;
	}
	float GetHeight_(){
		return height_gauge.getValue() / 100f;
	}
	float getDwell(){
		return dwell_gauge.getValue() / 100f;
	}
	int getPerNo(){
		return perno_gauge.getValue();
	}

	boolean ifMirror(){
		return mirror_box.getState();
	}
	boolean ifShowSiteSwap(){
		return ss_box.getState();
	}
	boolean ifShowBody(){
		return body_box.getState();
	}
	boolean ifSound(){
		return sound_box.getState();
	}

	void setIfMirror(boolean f){
		mirror_box.setState(f);
	}
	void setIfShowSiteSwap(boolean f){
		ss_box.setState(f);
	}
	void setIfShowBody(boolean f){
		body_box.setState(f);
	}
	void setIfSound(boolean f){
		sound_box.setState(f);
	}

	boolean isNewChoice(){
		boolean b;
		b = (prevIndex != patternList.getSelectedIndex());
		prevIndex = patternList.getSelectedIndex();
		return b;
	}
	void enableSwitches(){
		juggle_button.setEnabled(true);
		pause_button.setEnabled(true);
		patternList.setEnabled(true);
	}
	void disableSwitches(){
		juggle_button.setEnabled(false);
		pause_button.setEnabled(false);
		patternList.setEnabled(false);
	}
	void putMessage(String s1, String s2){
		putPrimaryMessage(s1);
		putSecondaryMessage(s2);
	}
	void putPrimaryMessage(String s){
		primarymessage.setText(s);
	}
	void putSecondaryMessage(String s){
		secondarymessage.setText(s);
	}
	void enableMenuBar(){
		menu_option.setEnabled(true);
		menu_quit.setEnabled(true);
	}
	void disableMenuBar(){
		menu_option.setEnabled(false);
		menu_quit.setEnabled(true);
	}
	static class JmjDialog extends Dialog implements ActionListener{
		private static final long serialVersionUID = 1275204082629828667L;
		public static final String STRING_LOAD = "Load a pattern file";
		public static final String STRING_SELECT_FILE = "Select a pattern file";
		public static final String STRING_SITESWAP = TRY_A_NEW_SITESWAP;
		public static final String STRING_MOTION = "Choose the motion";
		public static final String STRING_FORMATION = "Choose the formation";
		public static final String STRING_ABOUT = "About";

		public static final int LOAD_FILE = 1;
		public static final int TRY_SITESWAP = 2;
		public static final int CHOOSE_MOTION = 3;
		public static final int CHOOSE_FORMATION = 4;
		public static final int CHOOSE_ABOUT = 5;
		public static final int CHOOSE_DID_YOU_MEAN = 6; // "Did you mean..." feature (「もしかして」機能 )
		public static final int SELECT_FILE = 7;
		private int status;
		JmjController jc;

		private PatternFileList fileList;
		private MotionList motionList;
		private DidyoumeanList didyoumeanList;

		private Label label1;
		private Label label2;
		private Label label3;
		private Label label4;
		private Label label5;
		private Button ok, cancel;
		private TextField textField;
		private FormationList formationList;

		class FormationList extends ListWithQuicksort{
			private static final long serialVersionUID = 6987910603772207324L;
			void create(){
				if(jc.jmj.holder.countFormation() > getItemCount()){
					removeAll();
					jc.jmj.holder.rewindFormation();
					while(true){
						String nextFormation = jc.jmj.holder.nextFormation();
						if(nextFormation.length() == 0) break;
						add(nextFormation);
					}
					quickSort(0, getItemCount() - 1);
				}
			}
		}

		JmjDialog(JmjController j){
			super(j, false);
			setLayout(null);
			jc = j;
			fileList = jc.dialog_fileList;
			motionList = jc.dialog_motionList;
			didyoumeanList = jc.dialog_didyoumeanList;
			formationList = new FormationList();
			fileList.addActionListener(this);
			motionList.addActionListener(this);
			didyoumeanList.addActionListener(this);
			formationList.addActionListener(this);

			label1 = new Label();
			label2 = new Label();
			label3 = new Label();
			label4 = new Label();
			label5 = new Label();

			ok = jc.dialog_ok;
			cancel = jc.dialog_cancel;
			textField = jc.dialog_text;
			textField.addActionListener(this);
			add(ok);
			add(cancel);
			add(label1);
			ok.addActionListener(this);
			cancel.addActionListener(this);

			setVisible(false);
		}
		void popup(int mode){
			switch(status = mode){
				case LOAD_FILE:
					setSize(600, 160);
					label1.setText("Type in URL or filename to load:");
					textField.setText(Jmj.dirPath);
					label1.setBounds(10, 50, 380, 20);
					textField.setBounds(10, 70, 580, 25);
					ok.setLocation(100, 100);
					cancel.setLocation(230, 100);
					add(label1);
					add(textField);
					validate();
					setVisible(true);
					return;
				case SELECT_FILE:
					setSize(600, 310);
					label1.setText("Select file to load:");
					label1.setBounds(10, 50, 190, 20);
					add(label1);
					fileList.create();
					fileList.setBounds(10, 70, 580, 180);
					add(fileList);
					ok.setLocation(100, 260);
					cancel.setLocation(230, 260);
					validate();
					setVisible(true);
					return;
				case TRY_SITESWAP:
					setSize(600, 400);
					label1.setText("Type in a siteswap, and choose the motion ifyou like:");
					label2.setText("Arm motion:");
					motionList.create();
					textField.setText(new String());
					label1.setBounds( 10, 50, 380, 20);
					label2.setBounds(410, 50, 180, 20);
					motionList.setBounds(410, 70, 180, 300);
					textField.setBounds(10, 70, 380, 30);
					ok.setLocation(100, 120);
					cancel.setLocation(230, 120);
					add(label1);
					add(label2);
					add(motionList);
					add(textField);
					validate();
					setVisible(true);
					textField.requestFocusInWindow();
					return;
				case CHOOSE_MOTION:
					setSize(250, 400);
					label1.setText("Choose the motion:");
					motionList.create();
					label1.setBounds(10, 50, 230, 20);
					motionList.setBounds(10, 70, 230, 280);
					ok.setLocation(20, 360);
					cancel.setLocation(110, 360);
					add(label1);
					add(motionList);
					validate();
					setVisible(true);
					return;
				case CHOOSE_FORMATION:
					setSize(250, 400);
					label1.setText("Choose the formation:");
					formationList.create();
					label1.setBounds(10, 50, 230, 20);
					formationList.setBounds(10, 70, 230, 280);
					add(label1);
					add(formationList);
					ok.setLocation(20, 360);
					cancel.setLocation(110, 360);
					validate();
					setVisible(true);
					return;
				case CHOOSE_ABOUT:
					setSize(520, 200);
					label1.setText("JuggleMaster Version 1.60 Copyright (C) 1995-1996 Ken Matsuoka");
					label2.setText("JuggleMaster X Version 0.42 Copyright (C) 1996 MASUDA Kazuyoshi");
					label3.setText("JuggleMaster Java Version 1.03 Copyright (C) 1997-1999 Yuji Konishi,ASANUMA Nobuhiko");
					label4.setText("JuggleMaster Java Version 2.05 Copyright (C) 2005 Takumi Okada");
					label5.setText("JuggleMaster Java Version 2.13 Copyright (C) 2012 @tatt61880");
					label1.setBounds(10,  50, 380, 20);
					label2.setBounds(10,  70, 380, 20);
					label3.setBounds(10,  90, 500, 20);
					label4.setBounds(10, 110, 380, 20);
					label5.setBounds(10, 130, 380, 20);
					add(label1);
					add(label2);
					add(label3);
					add(label4);
					add(label5);
					remove(cancel);
					ok.setLocation(220, 150);
					validate();
					setVisible(true);
					return;
				case CHOOSE_DID_YOU_MEAN:
					remove(textField);
					remove(motionList);
					remove(label2);
					remove(label3);
					remove(label4);
					remove(label5);
					setSize(600, 500);
					label1.setText("Did you mean:");
					label1.setBounds(10, 50, 190, 20);
					didyoumeanList.setBounds(10, 70, 580, 380);
					ok.setLocation(100, 460);
					cancel.setLocation(230, 460);
					add(label1);
					add(didyoumeanList);
					validate();
					setVisible(true);
					return;
			}
		}

		public void actionPerformed(ActionEvent e){
			Object target = e.getSource();
			switch(status){
				case LOAD_FILE:
					if(target == textField || target == ok){
						setVisible(false);
						if(textField.getText().length() != 0 && !textField.getText().equals(Jmj.dirPath)){
							jc.jmj.openFile(textField.getText());
							return;
						}else{
							return;
						}
					}
					break;
				case TRY_SITESWAP:
					if(target == ok || target == textField){
						setVisible(false);
						if(textField.getText().length() != 0){
							if(motionList.getSelectedIndex() != -1){
								jc.jmj.motion = motionList.getSelectedItem();
							}else{
								jc.jmj.motion = Jmj.NORMAL;
							}
							int i = jc.patternList.getSelectedIndex();
							if(i != -1){
								jc.patternList.deselect(i);
								jc.isNewChoice();
							}
							synchronized(jc.jmj){
								String inputStr = textField.getText().replace(" ", "");
								if( jc.jmj.startJuggling(Jmj.SITESWAP_MODE, inputStr) == false){
									if(didyoumeanList.create(inputStr)){
										this.popup(CHOOSE_DID_YOU_MEAN);
									}
								}
							}
						}
						return;
					}
					break;
				case CHOOSE_MOTION:
					if(target == motionList || target == ok){
						setVisible(false);
						synchronized(jc.jmj){
							jc.jmj.startJuggling(Jmj.MOTION_MODE, motionList.getSelectedItem());
						}
						return;
					}
					break;
				case CHOOSE_FORMATION:
					// get XY-Data from hashtable, and set jmj.iXData[], jmj.iYData[]

					if(target == formationList || target == ok){
						setVisible(false);
						if(formationList.getSelectedIndex() != -1){
							jc.jmj.formation = formationList.getSelectedItem();
						}else{
							jc.jmj.formation = Jmj.FORMATION_BASIC;
						}
						synchronized(jc.jmj){
							jc.jmj.startJuggling(Jmj.FORMATION_MODE, jc.jmj.motion);
						}
						return;
					}
					break;
				case CHOOSE_ABOUT:
					if(target == ok){
						setVisible(false);
						return;
					}
					break;
				case CHOOSE_DID_YOU_MEAN:
					if(target == didyoumeanList || target == ok){
						setVisible(false);
						jc.jmj.startJuggling(Jmj.SITESWAP_MODE, didyoumeanList.getSelectedItem());
						return;
					}
					break;
				case SELECT_FILE:
					if(target == fileList || target == ok){
						setVisible(false);
						jc.jmj.openFile(fileList.getSelectedItem());
						return;
					}
					break;
			}

			if(target == cancel){
				setVisible(false);
			}
		}
	}
}
