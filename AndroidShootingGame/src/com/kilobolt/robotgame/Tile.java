package com.kilobolt.robotgame;

import android.graphics.Rect;

import com.kilobolt.framework.Image;

public class Tile {

	private int tileX, tileY, speedX;
	public int type;
	public Image tileImage;

	private Robot robot = GameScreen.getRobot();
	private Background bg = GameScreen.getBg1();

	private Rect r;

	public Tile(int x, int y, int typeInt) {
		tileX = x * 40;
		tileY = y * 40;

		type = typeInt;

		r = new Rect();

		if (type == 5) {
			tileImage = Assets.tiledirt;
		} else if (type == 8) {
			tileImage = Assets.tilegrassTop;
		} else if (type == 4) {
			tileImage = Assets.tilegrassLeft;

		} else if (type == 6) {
			tileImage = Assets.tilegrassRight;

		} else if (type == 2) {
			tileImage = Assets.tilegrassBot;
		} else {
			type = 0;
		}

	}

		public void update() {
			speedX = bg.getSpeedX() * 5;
			tileX += speedX;
			r.set(tileX, tileY, tileX+40, tileY+40);
	
			
			
			if (Rect.intersects(r, Robot.yellowRed) && type != 0) {
				checkVerticalCollision(Robot.rect, Robot.rect2);
				checkSideCollision(Robot.rect3, Robot.rect4, Robot.footleft, Robot.footright);
			}
	
		}

	public int getTileX() {
		return tileX;
	}

	public void setTileX(int tileX) {
		this.tileX = tileX;
	}

	public int getTileY() {
		return tileY;
	}

	public void setTileY(int tileY) {
		this.tileY = tileY;
	}

	public Image getTileImage() {
		return tileImage;
	}

	public void setTileImage(Image tileImage) {
		this.tileImage = tileImage;
	}

	public void checkVerticalCollision(Rect rtop, Rect rbot) {
		if (Rect.intersects(rtop, r)) {
			
		}

		if (Rect.intersects(rbot, r) && type == 8) {
			robot.setJumped(false);
			robot.setSpeedY(0);
			robot.setCenterY(tileY - 63);
		}
	}

	public void checkSideCollision(Rect rleft, Rect rright, Rect leftfoot, Rect rightfoot) {
		if (type != 5 && type != 2 && type != 0){
			if (Rect.intersects(rleft, r)) {
				robot.setCenterX(tileX + 102);
	
				robot.setSpeedX(0);
	
			}else if (Rect.intersects(leftfoot, r)) {
				
				robot.setCenterX(tileX + 85);
				robot.setSpeedX(0);
			}
			
			if (Rect.intersects(rright, r)) {
				robot.setCenterX(tileX - 62);
	
				robot.setSpeedX(0);
			}
			
			else if (Rect.intersects(rightfoot, r)) {
				robot.setCenterX(tileX - 45);
				robot.setSpeedX(0);
			}
		}
	}

}