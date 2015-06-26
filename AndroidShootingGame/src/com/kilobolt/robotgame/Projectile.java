package com.kilobolt.robotgame;

import android.graphics.Rect;


public class Projectile {

	private int x, y, speedX;
	private boolean visible;
	
	private Rect r;
	
	public Projectile(int startX, int startY){
		x = startX;
		y = startY;
		speedX = 7;
		visible = true;
		
		r = new Rect(0, 0, 0, 0);
	}
	
	public void update(){
		x += speedX;
		r.set(x, y, x+10, y+5);
		if (x > 800){
			visible = false;
			r = null;
		}
		if (visible){
			checkCollision();
		}
		
	}

	private void checkCollision() {
		
		if (Rect.intersects(r, GameScreen.hb.r)){
			visible = false;

			if (GameScreen.hb.health > 0) {
				GameScreen.hb.health -= 1;
			}
			if (GameScreen.hb.health == 0) {
				GameScreen.hb.setCenterX(-100);
				

			}

		}
		if (Rect.intersects(r, GameScreen.hb2.r)){
			visible = false;

			if (GameScreen.hb2.health > 0) {
				GameScreen.hb2.health -= 1;
			}
			if (GameScreen.hb2.health == 0) {
				GameScreen.hb2.setCenterX(-100);
				

			}

		}
	}


	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getSpeedX() {
		return speedX;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}

	public void setSpeedX(int speedX) {
		this.speedX = speedX;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	
}
