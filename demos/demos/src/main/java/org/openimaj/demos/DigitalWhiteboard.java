package org.openimaj.demos;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.MouseInputListener;

import org.openimaj.demos.DigitalWhiteboard.MODE;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.connectedcomponent.ConnectedComponentLabeler;
import org.openimaj.image.model.pixel.HistogramPixelModel;
import org.openimaj.image.pixel.ConnectedComponent;
import org.openimaj.image.pixel.Pixel;
import org.openimaj.math.geometry.line.Line2d;
import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.shape.Circle;
import org.openimaj.math.geometry.transforms.HomographyModel;
import org.openimaj.util.pair.IndependentPair;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.VideoDisplayListener;
import org.openimaj.video.capture.VideoCapture;

public class DigitalWhiteboard implements VideoDisplayListener<MBFImage>, MouseInputListener, KeyListener{
	private VideoCapture capture;
	private VideoDisplay<MBFImage> display;
	private JFrame drawingFrame;
	private MBFImage drawingPanel;
	private Runnable drawingUpdater;
	private ConnectedComponentLabeler labeler;
	List<MBFImage> learningFrames = new ArrayList<MBFImage>();
	private HistogramPixelModel model = null;
	private MODE mode = MODE.NONE;
	private HomographyModel homography = null;
	private List<IndependentPair<Point2d,Point2d>> homographyPoints = new ArrayList<IndependentPair<Point2d,Point2d>>();
	private List<IndependentPair<String,Point2d>> calibrationPoints = new ArrayList<IndependentPair<String,Point2d>>();
	private int calibrationPointIndex;
	private Point2dImpl previousPoint;
	
	enum MODE{
		MODEL,SEARCHING,NONE, LINE_CONSTRUCTING;
	}
	public DigitalWhiteboard() throws IOException{
		
		System.out.println(VideoCapture.getVideoDevices());
		capture = new VideoCapture(640,480,VideoCapture.getVideoDevices().get(1));
		display = VideoDisplay.createVideoDisplay(capture);
		display.addVideoListener(this);
		display.displayMode(true);
		display.getScreen().addKeyListener(this);
		GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		drawingPanel = new MBFImage(device.getDisplayMode().getWidth(),device.getDisplayMode().getHeight(),ColourSpace.RGB);
		drawingPanel.fill(RGBColour.WHITE);
		drawingFrame = DisplayUtilities.display(drawingPanel);
		drawingFrame.setBounds(640, 0, drawingPanel.getWidth(), drawingPanel.getHeight());
		drawingFrame.addKeyListener(this);
//		drawingFrame.setUndecorated(true);
		drawingFrame.setIgnoreRepaint(true);
		drawingFrame.setResizable(false);
		drawingFrame.setVisible(false);
//		drawingFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
//		device.setFullScreenWindow(drawingFrame);
		
		drawingUpdater = new Runnable(){

			@Override
			public void run() {
				while(true){
//					drawingPanel = new MBFImage(640,480,ColourSpace.RGB);
					drawWhiteboard(drawingPanel);
					DisplayUtilities.display(drawingPanel,drawingFrame);
					try {
						Thread.sleep(1000/30);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

		};
		Thread t = new Thread(drawingUpdater);
		t.start();
		
		labeler = new ConnectedComponentLabeler(ConnectedComponent.ConnectMode.CONNECT_8);
		labeler.THRESH = 0.98f;
		
		calibrationPoints.add(new IndependentPair<String,Point2d>("TOP LEFT",new Point2dImpl(0,0)));
		calibrationPoints.add(new IndependentPair<String,Point2d>("TOP RIGHT",new Point2dImpl(drawingPanel.getWidth(),0)));
		calibrationPoints.add(new IndependentPair<String,Point2d>("BOTTOM LEFT",new Point2dImpl(0,drawingPanel.getHeight())));
		calibrationPoints.add(new IndependentPair<String,Point2d>("BOTTOM RIGHT",new Point2dImpl(drawingPanel.getWidth(),drawingPanel.getHeight())));
		calibrationPointIndex = 0;
	}

	
	private void drawWhiteboard(MBFImage drawingPanel) {
//		drawingPanel.fill(RGBColour.WHITE);
		
	}
	@Override
	public void afterUpdate(VideoDisplay<MBFImage> display) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void beforeUpdate(MBFImage frame) {
		
//		FImage greyFramePart = Transforms.calculateIntensityNTSC(frame);
		FImage greyFramePart = frame.getBand(0);
		MBFImage greyFrame = new MBFImage(new FImage[]{greyFramePart.clone(),greyFramePart.clone(),greyFramePart.clone()});
		if(mode == MODE.MODEL){
			List<ConnectedComponent> labels = labeler.findComponents(greyFramePart);
			if(labels.size()>0){
				ConnectedComponent c = largestLabel(labels);
				Pixel centroid = c.calculateCentroidPixel();
				int distance = -1;
				if(this.homographyPoints.size() != 0){
					distance = distance(homographyPoints.get(homographyPoints.size()-1).firstObject(),centroid);
				}
				if(this.homographyPoints.size() == 0 || distance > 100){
					System.out.println("Point found at: " + centroid);
					System.out.println("Distance was: " + distance);
					IndependentPair<String, Point2d> calibration = this.calibrationPoints.get(this.calibrationPointIndex);
					System.out.println("Adding point for: " + calibration.firstObject());
					this.homographyPoints.add(new IndependentPair<Point2d, Point2d>(centroid,calibration.secondObject()));
					this.calibrationPointIndex++;
					if(this.calibrationPointIndex >= this.calibrationPoints.size()){
						this.homography.estimate(homographyPoints);
						this.mode = MODE.SEARCHING;
					}
					else{
						System.out.println("CURRENTLY EXPECTING POINT: " + this.calibrationPoints.get(this.calibrationPointIndex).firstObject());
					}
				}
				
				
			}
		}
		else if(mode == MODE.SEARCHING){
			List<ConnectedComponent> labels = labeler.findComponents(greyFramePart);
			if(labels.size()>0){
				this.mode = MODE.LINE_CONSTRUCTING;
				ConnectedComponent c = largestLabel(labels);
				Point2dImpl actualPoint = findActualPoint(c);
				drawingPanel.drawShapeFilled(new Circle((int)actualPoint.x, (int)actualPoint.y,5), RGBColour.BLACK);
				previousPoint = actualPoint;
			}
		}
		else if(mode == MODE.LINE_CONSTRUCTING){
			List<ConnectedComponent> labels = labeler.findComponents(greyFramePart);
			if(labels.size()>0){
				ConnectedComponent c = largestLabel(labels);
				Point2dImpl actualPoint = findActualPoint(c);
				drawingPanel.drawLine(new Line2d(previousPoint,actualPoint), 5, RGBColour.BLACK);
				previousPoint = actualPoint;
			}
			else{
				mode = MODE.SEARCHING;
				previousPoint = null;
			}
		}
		frame.internalAssign(greyFrame);
	}
	
	private Point2dImpl findActualPoint(ConnectedComponent c) {
		double[] centroidDouble = c.calculateCentroid();
		Point2dImpl centroid = new Point2dImpl((float)centroidDouble[0],(float)centroidDouble[1]);
		Point2dImpl actualPoint = centroid.transform(this.homography.getTransform());
		return actualPoint;
	}


	private ConnectedComponent largestLabel(List<ConnectedComponent> labels) {
		int max = 0;
		ConnectedComponent r = null;
		for(ConnectedComponent c : labels){
			if(c.getPixels().size() > max)
			{
				max = c.getPixels().size();
				r = c;
			}
		}
		return r;
	}


	private int distance(Point2d p1, Point2d p2) {
		double dx = p1.getX()-p2.getX();
		double dy = p1.getY()-p2.getY();
		return (int) Math.sqrt(dx*dx + dy*dy);
	}


	public static void main(String args[]) throws IOException{
		new DigitalWhiteboard();
	}


	@Override
	public void mouseClicked(MouseEvent arg0) {
		
	}


	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mouseDragged(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mouseMoved(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void keyPressed(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void keyReleased(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void keyTyped(KeyEvent event) {
		System.out.println("Got a key");
		if(event.getKeyChar() == 'c'){
			System.out.println("Modelling mode started");
			this.mode = MODE.MODEL;
			this.calibrationPointIndex = 0;
			homographyPoints .clear();
			this.homography  = new HomographyModel(8);
			
			System.out.println("CURRENTLY EXPECTING POINT: " + this.calibrationPoints.get(this.calibrationPointIndex).firstObject());
		}
		if(event.getKeyChar() == 'd' && this.homographyPoints.size() > 4){
			
		}
	}
}
