package com.mmog.players;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;

public class Imposter extends Player{

	public Imposter(int playerID) {
		super(playerID);
	}
	
	@Override
	public void draw(Batch batch) {
		super.draw(batch);
	}
}
