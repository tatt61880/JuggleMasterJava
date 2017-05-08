import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MessageBox extends JApplet{
	int patt[][] = new int[200][11];
	int patts[] = new int[200];
	int pattw;
	boolean isSync;
	String s;

	public void msgbx(String s1){
		JOptionPane.showMessageDialog((Component)null, s1, "SiteSwap Pattern",
				JOptionPane.INFORMATION_MESSAGE);
	}

	public void chkSSdlg(){
		String strtmp = "";
		int iCnt1, iCnt2, iMax = 0;

		strtmp = "InputStr(Siteswap): " + s + "\n";
		for(iCnt1=0; iCnt1<pattw; iCnt1++){
			if(iMax < patts[iCnt1]){
				iMax = patts[iCnt1];
			}
		}

		// jmj.patt[][], jmj.patts[], pattw の表示
		strtmp += "pattw=" + pattw;
		strtmp += "\njmj.patts[] = ";
		for(iCnt1=0; iCnt1<pattw; iCnt1++){
			strtmp += patts[iCnt1];
		}

		for(iCnt2=0; iCnt2<iMax; iCnt2++){
			strtmp += "\njmj.patt[][" + iCnt2 + "] =";
			for(iCnt1=0; iCnt1<pattw; iCnt1++){
				strtmp += " " + patt[iCnt1][iCnt2];
			}
		}
		strtmp += "\nisSync = " + isSync + "\n";
		msgbx(strtmp);
	}
}
