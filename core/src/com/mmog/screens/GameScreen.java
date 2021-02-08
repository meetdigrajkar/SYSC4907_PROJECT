package com.mmog.screens;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mmog.Client;
import com.mmog.players.CrewMember;
import com.mmog.players.DeadPlayer;
import com.mmog.players.Imposter;
import com.mmog.players.Player;

import box2dLight.ConeLight;
import box2dLight.Light;
import box2dLight.PointLight;
import box2dLight.RayHandler;

import com.mmog.tasks.*;

public class GameScreen extends AbstractScreen{

	TextButtonStyle tbs;
	BitmapFont font;
	private Viewport vp;
	OrthographicCamera cam;
	float width,height;

	Table table;

	//tiled map
	private TiledMap map;
	private OrthogonalTiledMapRenderer r;
	private MapObjects mapObjects;

	Task task;

	//lights
	RayHandler rayhandler;
	World world;

	BodyDef bodydef;
	Body body;
	public static Light light;

	BitmapFont f = new BitmapFont(Gdx.files.internal("UI/newlabelfont.fnt"));
	LabelStyle labelFontStyle = new LabelStyle(f, Color.WHITE);
	Label crewLabel = new Label("YOU'RE A CREW MEMBER! COMPLETE TASKS TO WIN", labelFontStyle);
	Label impLabel = new Label("YOU'RE AN IMPOSTER! SABOTAGE AND KILL TO WIN", labelFontStyle);

	FrameBuffer shadowBuffer,worldBuffer;
	String tasksString;

	public static final float TILE_SIZE = 1;
	
	public ArrayList<DeadPlayer> deadPlayers;

	public GameScreen() {
		super();
	}

	private ArrayList<Player> getYBasedSortedPlayers() {
		ArrayList<Player> allPlayers = new ArrayList();
		allPlayers.add(Client.getPlayer());

		for (Player p: Client.getPlayers())
		{
			if(p.getPlayerID() != -1) {
				allPlayers.add(p);
			}
		}

		//Render Based On Y-Axis to avoid poor sprite overlap.
		Collections.sort(allPlayers, new Comparator<Player>() {
			@Override
			public int compare(Player arg0, Player arg1) {
				return Float.compare(arg1.getY(), arg0.getY());

			}
		});
		return allPlayers;
	}

	public void createFrameBuffer() {
		shadowBuffer = new FrameBuffer(Format.RGB888, (int) width, (int) height, false);
		worldBuffer = new FrameBuffer(Format.RGB888, (int) width, (int) height, false);

		cam = new OrthographicCamera(worldBuffer.getWidth(), worldBuffer.getHeight());
		cam.setToOrtho(true);
		cam.update();
	}

	@Override
	public void show() {
		width = Gdx.graphics.getWidth();
		height = Gdx.graphics.getHeight();
		cam = new OrthographicCamera(width, height);
		cam.zoom = 0.45f;
		vp = new FitViewport(1920, 1080,cam);
		this.setViewport(vp);
		deadPlayers = new ArrayList<DeadPlayer>();
		
		if(Client.getPlayer() instanceof CrewMember) {
			((CrewMember) Client.getPlayer()).addTask(new AdminTask());
			((CrewMember) Client.getPlayer()).addTask(new ComsTask()); 
			((CrewMember) Client.getPlayer()).addTask(new ReactorTask());
			((CrewMember) Client.getPlayer()).addTask(new ElectricalTask());
		}

		map = new TmxMapLoader().load("MapAreas/mapfiles/map.tmx");
		r = new OrthogonalTiledMapRenderer(map);

		Client.getPlayer().setCollisionLayer((TiledMapTileLayer) map.getLayers().get(0));
		Client.getPlayer().setPosition(35 * Client.getPlayer().getCollisionLayer().getTileWidth(), (Client.getPlayer().getCollisionLayer().getHeight() - 10) * Client.getPlayer().getCollisionLayer().getTileHeight());

		cam.setToOrtho(false);
		cam.position.set(Client.getPlayer().getX() + (Client.getPlayer().getWidth() * 2), Client.getPlayer().getY() + (Client.getPlayer().getHeight()), 0);
		cam.update();

		r.getBatch().setProjectionMatrix(cam.combined);

		//light and box stuff
		this.world = new World(new Vector2(0,0),false);

		//map objects
		buildBuildingsBodies();

		//ray handler
		rayhandler = new RayHandler(world);
		rayhandler.setAmbientLight(0.01f);
		RayHandler.useDiffuseLight(true);

		//cone light for the player
		//change the light distance when the imposter sends the sabotage request
		light = new ConeLight(rayhandler,120,Color.WHITE, 180,Client.getPlayer().getX(), Client.getPlayer().getY(),360,360);
		light.setPosition(Client.getPlayer().getX()+ 17,Client.getPlayer().getY()+ 17);

		//createFrameBuffer();
	}

	private void buildBuildingsBodies() {
		mapObjects = map.getLayers().get("wall layer").getObjects();

		for(MapObject mo: mapObjects) {
			//System.out.println(mo.getColor());
			Rectangle rectangle = ((RectangleMapObject)mo).getRectangle();

			//create a dynamic within the world body (also can be KinematicBody or StaticBody
			BodyDef bodyDef = new BodyDef();
			bodyDef.type = BodyDef.BodyType.DynamicBody;
			Body body = world.createBody(bodyDef);

			//create a fixture for each body from the shape
			Fixture fixture = body.createFixture(getShapeFromRectangle(rectangle),1);
			fixture.setFriction(0.1F);

			//setting the position of the body's origin. In this case with zero rotation
			body.setTransform(getTransformedCenterForRectangle(rectangle),0);
		}		
	}

	public static Shape getShapeFromRectangle(Rectangle rectangle){
		PolygonShape polygonShape = new PolygonShape();
		polygonShape.setAsBox(rectangle.width*0.5F/ TILE_SIZE,rectangle.height*0.5F/ TILE_SIZE);
		return polygonShape;
	}

	public static Vector2 getTransformedCenterForRectangle(Rectangle rectangle){
		Vector2 center = new Vector2();
		rectangle.getCenter(center);
		return center.scl(1/TILE_SIZE);
	}

	@Override
	public void buildStage() {
	}

	public RayHandler getRayHandler() {
		return rayhandler;
	}

	public void update(float delta) {
		cam.position.set(Client.getPlayer().getX() + (Client.getPlayer().getWidth() * 2), Client.getPlayer().getY() + (Client.getPlayer().getHeight()), 0);
		cam.unproject(new Vector3(Client.getPlayer().getX(), Client.getPlayer().getY(), 0));
		this.getCamera().update();
		r.getBatch().setProjectionMatrix(cam.combined);
	}

	@Override
	public void render(float delta) {
		//clear the previous screen
		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		world.step(1/60f, 6, 2);
		Gdx.input.setInputProcessor(this);
		//updates the camera position
		update(delta);

		//map renderer
		r.setView(cam);
		r.render();
		
		r.getBatch().begin();
		//draw all the dead bodies
		for(DeadPlayer dp: deadPlayers) {
			dp.draw(r.getBatch());
		}
		
		//draw all the other players
		for (Player p : getYBasedSortedPlayers())
		{
			if(p.isDead && !p.addedToDead) {
				DeadPlayer dp = new DeadPlayer((int)p.getX(),(int)p.getY());
				dp.setName(p.getPlayerName());
				
				deadPlayers.add(dp);
				p.addedToDead = true;
			}
			
			//ghosts players can see everyone
			if(Client.getPlayer().isDead) {
				p.draw(r.getBatch());
				
			}
			//alive players can see ONLY alive players 
			else if(!Client.getPlayer().isDead) {
				if(!p.isDead) {
					p.draw(r.getBatch());
				}
			}
		}
		r.getBatch().end();
		
		light.setPosition(Client.getPlayer().getX() + 17, Client.getPlayer().getY() + 17);
		rayhandler.setCombinedMatrix(cam);
		rayhandler.updateAndRender();

		detectingKeyPresses();

		r.getBatch().begin();
		
		//if the player is a crew member, call setCurrentTask() on the player which sets the players current task if they have tried to start a task
		if(Client.getPlayer() instanceof CrewMember) {
			
			//check for collision on a dead body
			for(DeadPlayer dp: deadPlayers) {
				if(Client.getPlayer().playerRec.overlaps(dp.getDeadPlayerRec())) {
					System.out.println("FOUND DEAD BODY: @name: " + dp.getName());
					
					((CrewMember) Client.getPlayer()).reportButton.setVisible(true);
				}
			}

			((CrewMember) Client.getPlayer()).drawTasks(r.getBatch());

			//if the player has a current task, render the task screen ui
			if(((CrewMember) Client.getPlayer()).getCurrentTask() != null) {
				//based on the task the player is doing, render the appropriate task 
				task = ((CrewMember) Client.getPlayer()).getCurrentTask();

				if(task instanceof AdminTask) {
					((AdminTask) task).render(r.getBatch());
				}
				if(task instanceof ReactorTask) {
					((ReactorTask) task).render(r.getBatch());
				}
				if(task instanceof ElectricalTask) {
					((ElectricalTask) task).render(r.getBatch());
				}
				if(task instanceof ComsTask) {
					((ComsTask) task).render(r.getBatch());
				}
			}
			
		}
		else if(Client.getPlayer() instanceof Imposter) {
			light.setDistance(550);

			((Imposter) Client.getPlayer()).drawUI(r.getBatch());
			
			//check if the local player overlapped any players
			for(Player p: getYBasedSortedPlayers()) {
				if(!Client.getPlayer().getPlayerName().equals(p.getPlayerName())) {
					if(!p.isDead && !Client.getPlayer().isDead && Client.getPlayer().playerRec.overlaps(p.playerRec)) {
						if(Gdx.input.isKeyPressed(Keys.SPACE)) {
							try {
								p.isDead = true;
								System.out.println("PLAYER KILLED: " + p.getPlayerName());
								Client.sendPlayerKilled(p.getPlayerName());
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}
			}
			
			//check for collision on a dead body
			for(DeadPlayer dp: deadPlayers) {
				if(Client.getPlayer().playerRec.overlaps(dp.getDeadPlayerRec())) {
					System.out.println("FOUND DEAD BODY: @name: " + dp.getName());
					
					((Imposter) Client.getPlayer()).reportButton.setVisible(true);
				}
			}
		}
		
		try {
			Client.getPlayer().render(Gdx.graphics.getDeltaTime());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		r.getBatch().end();
	}

	public void detectingKeyPresses() {
		//exiting the game
		if(Gdx.input.isKeyPressed(Keys.ESCAPE)) {
			System.out.println("Back To Main Screen!");
			Client.removeClient();
		}
	}

	@Override
	public void resize(int width, int height) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub

	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub

	}

	@Override
	public void hide() {
		// TODO Auto-generated method stub
		dispose();
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		super.dispose();
	}

	public World getWorld() {
		return this.world;
	}

}