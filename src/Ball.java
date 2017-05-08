public class Ball{
	static Jmj jmj;

	int bh;
	int gx,gy;
	int gx0,gy0;
	int gx1,gy1;
	int c;
	int c0;
	int chand;
	int thand;
	int st;

	int hand_x, hand_y;

	int tPer, cPer;   // tPer : throw person,  cPer : catch person

	final static int OBJECT_HAND = 0x01;
	final static int OBJECT_UNDER = 0x02;
	final static int OBJECT_MOVE = 0x04;
	final static int OBJECT_MOVE2 = 0x08;

	int juggle(){
		int tp, flag = 0, i, tpox = 0, rpox = 0, tpoz = 0, rpoz = 0;
		long x, y;
		float fx;

		gx0 = gx;
		gy0 = gy;

		int iFlag = c;
		if(c < 0){
			if(jmj.timeCount >= -c * jmj.tw){
				c = -c;
			}
		}
		while(true){
			tp = (int)(jmj.timeCount - jmj.tw * Math.abs(c));
			if(tp <jmj.aw){
				break;
			}
			st &= ~OBJECT_UNDER;
			c0 = c;

			if((st & OBJECT_HAND) != 0){
				c += 2;
				flag = 1;
			}else{
				int t;
				t = c;
				if(jmj.isSync){
					if(jmj.mirror && chand == 0){
						t++;
					}else if(!jmj.mirror && chand != 0){
						t++;
					}
				}

				t %= jmj.pattw;
				bh = jmj.patt[t][jmj.r[t]];
				c+= Math.abs(bh);
				if(++jmj.r[t] >= jmj.patts[t]){
					jmj.r[t] = 0;
				}

				thand = chand;
				if(bh % 2 == 1 || bh < 0){
					chand = 1 - chand;
				}
				flag = 1;
			}
		}

		// Determine Catch&Throw person

		if(c < 0){
			cPer = (int)(-c / 2) % jmj.iPerNo;

		}else{
			cPer = (int)(c / 2) % jmj.iPerNo;
		}

		if(c0 < 0){
			tPer = (int)(-c0 / 2) % jmj.iPerNo;

		}else{
			tPer = (int)(c0 / 2) % jmj.iPerNo;
		}

		if(iFlag == Math.abs(bh)){
			cPer = (Math.abs(bh) / 2) % jmj.iPerNo;
			tPer = (Math.abs(bh) / 2) % jmj.iPerNo;
		}

		if((st & OBJECT_HAND) != 0){
			tPer = jmj.jPerNo;
			cPer = jmj.jPerNo;
		}

		if( c >= 0 && tp >= 0 && (st & OBJECT_UNDER) == 0){
			st |= OBJECT_UNDER;
			if((st & OBJECT_HAND) != 0){
				if((st & OBJECT_MOVE2) != 0){
					st |= OBJECT_MOVE;
					st &= ~OBJECT_MOVE2;
				}else{
					st &= ~OBJECT_MOVE;
				}
			}else{
				int t = c;
				if(jmj.isSync){
					if(jmj.mirror && chand == 0){
						t++;
					}else if(!jmj.mirror && chand != 0){
						t++;
					}
				}
				t %= jmj.pattw;

				if(bh == 1){
					st |= OBJECT_MOVE;
				}else{
					st &= ~OBJECT_MOVE;
				}

				for(i = 0; i < jmj.patts[t]; i++){
					int h = jmj.patt[t][i];
					if(h == 1){
						if(jmj.mirror == false){
							if(chand != 0){
								jmj.lhand[(cPer+1) % jmj.iPerNo].st |= OBJECT_MOVE2;
							}else{
								jmj.rhand[(cPer+1) % jmj.iPerNo].st |= OBJECT_MOVE2;
							}
						}else{
							if(chand != 0){
								jmj.lhand[(cPer + jmj.iPerNo - 1) % jmj.iPerNo].st |= OBJECT_MOVE2;
							}else{
								jmj.rhand[(cPer + jmj.iPerNo - 1) % jmj.iPerNo].st |= OBJECT_MOVE2;
							}
						}
					}

					// ボールを投げないのは
					// ・一人 かつ h=2
					// ・複数 かつ h=iPerNo*2 かつ モーションが同じ場合

					//	  if(h != jmj.iPerNo * 2){
					//	  if(h != 2){
					//	  if(jmj.iPerNo != 1 || h != 2){

					//jmj.MessageBox("hand_pos前 1\nc:" + c + "\nchand:" + chand + "\ncPer:" + cPer);

					hand_pos(c, chand, cPer);
					tpox = hand_x;
					tpoz = hand_y;

					//jmj.MessageBox("hand_pos前 2c:" + c + "\nchand:" + chand + "\ncPer:" + cPer + "\nh:" + h);

					hand_pos(c+Math.abs(h), chand, cPer);
					rpox = hand_x;
					rpoz = hand_y;

					//jmj.MessageBox("hand_pos後");

					if((jmj.iPerNo == 1 && h == 2) ||
							(jmj.iPerNo > 1
							 && h == jmj.iPerNo * 2
							 && tpox == rpox
							 && tpoz == rpoz )){

					}else{
						// ボールを投げる
						if(chand != 0){
							jmj.rhand[cPer].st |= OBJECT_MOVE2;
						}else{
							jmj.lhand[cPer].st |= OBJECT_MOVE2;
						}
						st |= OBJECT_MOVE;
					}
				}
			}
		}

		if((st & OBJECT_UNDER) != 0 && bh != 1){
			tPer = cPer;
		}

		if(c < 0){
			tPer = cPer;
		}

		if((st & OBJECT_MOVE) == 0){
			if(c < 0){
				hand_pos(-c, chand, tPer);
				tpox = hand_x;
				tpoz = hand_y;
				rpox = tpox;
				rpoz = tpoz;
			}else{
				if((st & OBJECT_UNDER) != 0){
					hand_pos(c, chand, tPer);
					tpox = hand_x;
					tpoz = hand_y;
					hand_pos(c + 2*jmj.iPerNo, chand, cPer);
					rpox = hand_x;
					rpoz = hand_y;
					if(tpox != rpox || tpoz != rpoz){
						hand_pos(c + 1, chand, cPer);
						rpox = hand_x;
						rpoz = hand_y;
						if(tpox != rpox || tpoz != rpoz){
							st |= OBJECT_MOVE;
						}
					}
				}else{
					hand_pos(c - 2, chand, tPer);
					tpox = hand_x;
					tpoz = hand_y;
					hand_pos(c, chand, cPer);
					rpox = hand_x;
					rpoz = hand_y;
					if(tpox != rpox || tpoz != rpoz){
						hand_pos(c - 1, chand, tPer);
						tpox = hand_x;
						tpoz = hand_y;
						if(tpox != rpox || tpoz != rpoz){
							st |= OBJECT_MOVE;
						}
					}
				}
			}
		}
		if((st & OBJECT_MOVE) != 0){
			if(bh == 1){
				hand_pos(c0 + 1, thand, tPer);
				tpox = hand_x;
				tpoz = hand_y;
				hand_pos(c + 1, chand, cPer);
				rpox = hand_x;
				rpoz = hand_y;
			}else if((st & OBJECT_UNDER) != 0){
				hand_pos(c, chand, tPer);
				tpox = hand_x;
				tpoz = hand_y;
				hand_pos(c + 1, chand, cPer);
				rpox = hand_x;
				rpoz = hand_y;
			}else{
				hand_pos(c0 + 1, thand, tPer);
				tpox = hand_x;
				tpoz = hand_y;
				hand_pos(c, chand, cPer);
				rpox = hand_x;
				rpoz = hand_y;
			}
		}

		// add position data on Throw & Catch person
		if((st & OBJECT_HAND) == 0){
			if(jmj.mirror == false){
				tpox += jmj.iXData[tPer] * 40 / jmj.PXY;
				rpox += jmj.iXData[cPer] * 40 / jmj.PXY;
			}else{
				tpox -= jmj.iXData[tPer] * 40 / jmj.PXY;
				rpox -= jmj.iXData[cPer] * 40 / jmj.PXY;
			}
			tpoz += jmj.iYData[tPer] * 20 / jmj.PXY;
			rpoz += jmj.iYData[cPer] * 20 / jmj.PXY;

		}else{
			if(jmj.mirror == false){
				tpox += jmj.iXData[jmj.jPerNo] * 40 / jmj.PXY;
				rpox += jmj.iXData[jmj.jPerNo] * 40 / jmj.PXY;
			}else{
				tpox -= jmj.iXData[jmj.jPerNo] * 40 / jmj.PXY;
				rpox -= jmj.iXData[jmj.jPerNo] * 40 / jmj.PXY;
			}
			tpoz += jmj.iYData[jmj.jPerNo] * 20 / jmj.PXY;
			rpoz += jmj.iYData[jmj.jPerNo] * 20 / jmj.PXY;
		}

		if( (st & OBJECT_HAND) == 0 && c < 0 ){
			if(tpox == 0){
				fx = 0;
				y = (long)((float)tpoz * jmj.dpm / 20
						- (long)tp * jmj.dpm / 12 / jmj.tw);
			}else{
				if(tpox > 0){
					fx = (float)tpox / 10 - (float)tp / 6 / jmj.tw;
				}else{
					fx = (float)tpox / 10 + (float)tp / 6 / jmj.tw;
				}
				y = (int)((float)tpoz * jmj.dpm / 20);
			}
		}else if((st & OBJECT_MOVE) == 0){
			fx = (float)tpox / 10;
			y = (int)((float)tpoz * jmj.dpm / 20);
		}else{
			if(bh==1){
				fx = (float)(tp - jmj.aw) / jmj.tw * 2 + 1;
				y = (long)((float)jmj.high[1] * (1 - square(fx)));
			}else if((st & OBJECT_UNDER) != 0){
				fx = (float)tp / jmj.aw * 2 - 1;
				y = (long)((float)jmj.high[0] * (1 - square(fx)));
			}else{
				fx = (float)tp / (jmj.tw * Math.abs(bh) - jmj.aw) * 2 + 1;
				y = (long)((float)jmj.high[Math.abs(bh)] * (1 - square(fx)));
			}

			y += (float)(fx * (rpoz - tpoz) + rpoz + tpoz)
				* jmj.dpm / 40;
			fx = (fx * (rpox - tpox) + rpox + tpox) / 20;
		}

		x = (long)(fx * jmj.dpm * jmj.KW);
		gx = (int)(x - 11);

		if((st & OBJECT_HAND) != 0){
			if(chand != 0){
				gx += jmj.hand_x;
			}else{
				gx -= jmj.hand_x;
			}
			y -= jmj.hand_y;
		}

		gy = (int)(jmj.base - y - 11);

		return flag;
	}

	void hand_pos(int c, int h, int person){
		int a;
		int a2, a3;
		if(jmj.mirror){
			if(!jmj.isSync &&  h != 0){
				c--;
			}
			if((c & 1) != 0){
				a2 = (--c + h);
				a3 = a2 / (jmj.iPerNo * 2);
				a3 = a3 * 2;
				if((a2 / 2) * 2 != a2) a3++;
				a = (a3 % (jmj.motionlength[person] / 4)) * 4 + 2;
			}else{
				a2 = (c + h);
				a3 = a2 / (jmj.iPerNo * 2);
				a3 = a3 * 2;
				if((a2 / 2) * 2 != a2) a3++;
				a = (a3 % (jmj.motionlength[person] / 4)) * 4;
			}
		}else{
			if(!jmj.isSync && h == 0){
				c--;
			}if((c & 1) != 0){
				a2 = (c - h);
				a3 = a2 / (jmj.iPerNo * 2);
				a3 = a3 * 2;
				if((a2 / 2) * 2 != a2) a3++;
				a = (a3 % (jmj.motionlength[person] / 4)) * 4 + 2;
			}else{
				a2 = (c + 1 - h);
				a3 = a2 / (jmj.iPerNo * 2);
				a3 = a3 * 2;
				if((a2 / 2) * 2 != a2) a3++;
				a = (a3 % (jmj.motionlength[person] / 4)) * 4;
			}
		}
		if(h != 0){
			hand_x = jmj.motionarray2[person][a];
		}else{
			hand_x = -jmj.motionarray2[person][a];
		}
		hand_y = jmj.motionarray2[person][a + 1];
	}

	void printOn(){
		System.out.println("bh: " + bh);
		System.out.println("gx, gy: " + gx + ", " + gy);
		System.out.println("gx0, gy0: " + gx0 + ", " + gy0);
		System.out.println("gx1, gy1: " + gx1 + ", " + gy1);
		System.out.println("c: " + c);
		System.out.println("c0: " + c0);
		System.out.println("chand: " + chand);
		System.out.println("thand: " + thand);
		System.out.println("st: " + st);
	}
	float square(float x){
		return x * x;
	}
}
