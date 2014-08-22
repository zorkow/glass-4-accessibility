package imageRegistration;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import strokeData.Coord;
import videoProcessing.ProcessImage;


/**
 * Class to define a specific type of zone/area or rectangular region of interest within an image.  A zone
 * is defined firstly by a Rect object which defines the location of the zone within an image and dimensions 
 * of the zone, and secondly by the zone type.  Zone type can comprise either: 0=Whiteboard, 1=Text, or 
 * 2=Other. 
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-08-20
 */
public class Zone {

	private Rect zoneAtt;	//the location and dimensions of the zone.
	private int zoneType;	//0 = Whiteboard, 1 = Text, 2 = Other
	
	private static final int WHITE_THRESHOLD = 252;
	private static final int DARK_THRESHOLD1 = 80;
	private static final double DARK_THRESHOLD2 = 0.20;
	private static final double TEXT_THRESHOLD = 1.5;
	
	/**
	 * Constructor for Zone objects.
	 * 
	 * @param zoneAtt - the Rect defining the location and dimension of the zone.
	 * @param zoneType - the type of zone (0 = Whiteboard, 1 = Text, 2 = Other)
	 */
	public Zone(Rect zoneAtt, int zoneType) {
		this.zoneAtt = zoneAtt;
		this.zoneType = zoneType;
	}
	
	
	public Rect getZoneAtt() {
		return zoneAtt;
	}

	public void setZoneAtt(Rect zoneAtt) {
		this.zoneAtt = zoneAtt;
	}

	public int getZoneType() {
		return zoneType;
	}

	public void setZoneType(int zoneType) {
		this.zoneType = zoneType;
	}	
	
	
	/**
	 * Method to count the number of a particular type of zone within a list of zones.
	 * 
	 * @param zones - the list of zones to look through.
	 * @param typeRef - the type of zone to count.
	 * @return the number of zones of type typeRef within the list of Zone zones
	 */
	public static int numberOfType(List<Zone> zones, int typeRef) {
		int count = 0;
		
		for(Zone elem : zones) {
			if(elem.getZoneType()==typeRef) {
				count++;
			}
		}
		
		return count;
	}
	
	
	/**
	 * Method to scan through a grayscale image with square zones of dimension windowSize overlapping 
	 * at spacing windowSize/2 and to classify each zone accordingly.  The method returns a list of all the
	 * zones within the image with the ascertained classification.
	 * 
	 * @param imgGray - the img to look through and classify.
	 * @param windowSize - the dimension of the square zones to use.
	 * @return a list of Zone objects overlapping at windowSize/2 spacing for the whole imgGray.
	 */
	public static List<Zone> classifyZones(Mat imgGray, int windowSize) {
		
		List<Zone> zones = new ArrayList<Zone>();
		
		//detect the edges in the image, dilate them slightly to sort out any discontinuities
		//then convert the edges image to black edges on a white background.
		Mat edges = ProcessImage.cannyEdge(imgGray, 150, 75);
		edges = ProcessImage.dilate(edges, 3);
		Imgproc.threshold(edges, edges, 127, 255, Imgproc.THRESH_BINARY_INV);
		
		//move the window across the, with windows overlapping at half the windowSize. 
		for(int i=0; i+windowSize<imgGray.width(); i=i+windowSize/2) {
			for(int j=0; j+windowSize<imgGray.height(); j=j+windowSize/2) {
				
				//extract the window from the source image and the edges image.
				Mat window = imgGray.submat(j,j+windowSize,i,i+windowSize);
				Mat edgesWindow = edges.submat(j,j+windowSize,i,i+windowSize);

				Rect r = new Rect(windowSize, windowSize, new Coord(i,j));
				
				//look for dark pixels (i.e. non-whiteboard).  A significant number of these would indicate
				//a likely 'foreign' object (not text or whiteboard).
				Mat windowThresh = new Mat();
				Imgproc.threshold(window, windowThresh, DARK_THRESHOLD1, 1, Imgproc.THRESH_BINARY_INV);
				double[] val = Core.sumElems(windowThresh).val;
				double avDarkPixels = val[0] / (windowSize*windowSize);
				
				//calculate the average pixel intensity of the edges window 
				//(255 would represent a completely white image and 0 a completely black image)
				double[] valEdges = Core.sumElems(edgesWindow).val;
				double avPixelIntensityEdges = valEdges[0];
				avPixelIntensityEdges = avPixelIntensityEdges / (windowSize*windowSize);

				if(avDarkPixels>DARK_THRESHOLD2) {
					zones.add(new Zone(r, 2));	//classify the zone as 'other' (non-text, non-whiteboard)
				} else if(avPixelIntensityEdges>=WHITE_THRESHOLD) {
					zones.add(new Zone(r, 0));	//classify the zone as whiteboard.
				} else {
					double errorVal = processWindow(window, edgesWindow);
					if(errorVal<TEXT_THRESHOLD) {
						zones.add(new Zone(r, 1));	//classify the zone as text.
					} else {
						zones.add(new Zone(r, 2));	//classify the zone as 'other' (non-text, non-whiteboard)
					}
				}
				
			}
		}

		return zones;
		
	}

	/**
	 * Finds the difference between an image and the edges extracted from that image.  
	 * It is assumed that an image of text only will have a low difference between itself and its edges and 
	 * hence the method should return a lower value for images containing text only.
	 * 
	 * @param window - the source image
	 * @param edges - the edges detected in the source image
	 * @return a value indicating how likely the frame contains text only (the LOWER the value the higher the
	 * likelihood).
	 */
	private static double processWindow(Mat window, Mat edges) {
		
		Mat winCopy = new Mat();
		Core.normalize(window, winCopy, 0, 255, Core.NORM_MINMAX);
		Imgproc.threshold(winCopy, winCopy, 127, 255, Imgproc.THRESH_BINARY+Imgproc.THRESH_OTSU);

		Mat out = new Mat();
		Core.subtract(edges, winCopy, out);

		double[] val = Core.sumElems(out).val;
		double sum = 0;
		for(int k=0; k<val.length; k++) {
			sum += val[k];
		}
		return sum/(window.height()*window.width());
	}
	
	/**
	 * Method to draw a list of zones on a background image to a specified file.  
	 * Zones of type 0 (whiteboard) are drawn in Red, of type 1 (text) are drawn in green, and of type 2
	 * (other) are drawn in black.
	 * 
	 * @param img - the image to draw the zones on.
	 * @param zones - the list of zones to draw.
	 * @param file - the full file path including extension of where to write the image file to.
	 */
	public static void drawZones(Mat img, List<Zone> zones, String file) {
		
		Mat copy = new Mat();
		img.copyTo(copy);
		
		for(int i=0; i<zones.size(); i++) {
			int type = zones.get(i).getZoneType();
			Rect r = zones.get(i).getZoneAtt();
			
			if(type==0) {
				ProcessImage.drawRedRect(copy, new Point(r.getLocation().getX(), r.getLocation().getY()), 
						r.getWidth(), r.getHeight());
			} else if(type==1) {
				ProcessImage.drawGreenRect(copy, new Point(r.getLocation().getX(), r.getLocation().getY()), 
						r.getWidth(), r.getHeight());
			} else {
				ProcessImage.drawBlackRect(copy, new Point(r.getLocation().getX(), r.getLocation().getY()), 
						r.getWidth(), r.getHeight());
			}
		}
		
		Highgui.imwrite(file, copy);
		
	}
	
	/**
	 * Method to remove all of the zones of a specified type from a list of zones. The original list of
	 * zones is unaffected.
	 * 
	 * @param zones - the list of zones to filter (i.e. remove a certain type).
	 * @param type - the type to remove.
	 * @return the filtered list of zones with all zones of type 'type' removed.
	 */
	public static List<Zone> removeZoneOfType(List<Zone> zones, int type) {
		
		List<Zone> filtered = new ArrayList<Zone>();
		for(Zone elem : zones) {
			if(elem.getZoneType()!=type) {
				filtered.add(elem);
			}
		}
		
		return filtered;
	}
	
	/**
	 * Method to filter a list of zones to only contain zones of a specified type.  The original list is
	 * unaffected.
	 * 
	 * @param zones - the list of zones to filter.
	 * @param type - the zone type to include in the output list (all other zone types are filtered out)
	 * @return the filtered list of zones with only zones of type 'type' included.
	 */
	public static List<Zone> getZoneOfType(List<Zone> zones, int type) {
		
		List<Zone> filtered = new ArrayList<Zone>();
		for(Zone elem : zones) {
			if(elem.getZoneType()==type) {
				filtered.add(elem);
			}
		}
		
		return filtered;
	}
	
	/**
	 * Method to find a large Rect within a list of zones.  Method may not return the optimal result, but 
	 * will return a satisfactory result.
	 * The method favours rectangles closer to a square in shape over those that have one long side where
	 * the two have the same area.
	 * 
	 * @param zones - the list of all zones to search.
	 * @return a Rect object describing the location and dimensions of the output rectangle.
	 */
	public static Rect findLargestRect(List<Zone> zones) {
		
		int minX=Integer.MAX_VALUE, maxX=Integer.MIN_VALUE; 
		int minY=Integer.MAX_VALUE, maxY=Integer.MIN_VALUE;
		int windowSizeX = zones.get(0).getZoneAtt().getWidth();
		int windowSizeY = zones.get(0).getZoneAtt().getHeight();
		int spacingX = windowSizeX/2;
		int spacingY = windowSizeY/2;
		
		for(int i=0; i<zones.size(); i++) {
			Coord c = zones.get(i).getZoneAtt().getLocation();
			minX = (c.getX()<minX) ? c.getX() : minX;
			minY = (c.getY()<minY) ? c.getY() : minY;
			maxX = (c.getX()+windowSizeX>maxX) ? c.getX()+windowSizeX : maxX;
			maxY = (c.getY()+windowSizeY>maxY) ? c.getY()+windowSizeY : maxY;
		}
		
		byte[][] zoneGrid = new byte[(maxX-minX)/spacingX][(maxY-minY)/spacingY];
		
		for(int i=0; i<zones.size(); i++) {
			int xval = (zones.get(i).getZoneAtt().getLocation().getX()-minX)/spacingX;
			int yval = (zones.get(i).getZoneAtt().getLocation().getY()-minY)/spacingY;
			zoneGrid[xval][yval] = 1;
			zoneGrid[xval+1][yval] = 1;
			zoneGrid[xval][yval+1] = 1;
			zoneGrid[xval+1][yval+1] = 1;
		}
		
		Rect bestRect = new Rect(0,0, new Coord(0,0));
		for(int i=0; i<zoneGrid.length; i++) {
			for(int j=0; j<zoneGrid[0].length; j++) {
				if(zoneGrid[i][j]==1) {
					Rect r = findSize(zoneGrid, i, j);
					if(r.getArea()>bestRect.getArea()) {
						bestRect = r;
					} else if(r.getArea()==bestRect.getArea()) {
						int bestMinSide = Math.min(bestRect.getHeight(), bestRect.getWidth());
						int rMinSide = Math.min(r.getHeight(), r.getWidth());
						if(rMinSide>bestMinSide) {
							bestRect=r;
						}
					}
				}
			}
		}
		
		bestRect.setLocation(new Coord(bestRect.getLocation().getX()*spacingX+minX, bestRect.getLocation().getY()*spacingY+minY));
		bestRect.setWidth(bestRect.getWidth()*spacingX);
		bestRect.setHeight(bestRect.getHeight()*spacingY);
		
		System.out.println("Largest area = " + bestRect.getArea());
		return bestRect;
		
	}
	
	/**
	 * Helper method to findLargestRect which finds the maximum rectangle by area in a grid of 1's and 0's
	 * where 1's are valid squares to include in the rectangle and 0's are not.  The rectangle has its 
	 * top left corner at the locations specified by parameters 'a' and 'b'.  
	 * 
	 * @param grid - the grid of 1's and 0's to search for the maximum rectangle.
	 * @param a - the top left corner of the rectangle in the first dimension of the grid.
	 * @param b - the top left corner of the rectangle in the second dimension of the grid.
	 * @return a large rectangle within the grid starting from location [a][b].
	 */
	private static Rect findSize(byte[][] grid, int a, int b) {
		
		int limitY=0;
		for(int i=b+1; i<grid[0].length; i++) {
			if(grid[a][i]==1) {
				limitY++;
			} else {
				break;
			}
		}
		
		byte[] lengths = new byte[limitY+1];
		
		for(int i=b; i<b+lengths.length; i++) {
			int val = a;
			while(val<grid.length && grid[val][i]==1) {
				lengths[i-b]++;
				val++;
			}
		}
		
		int maxSize = lengths[0];
		Rect r = new Rect(maxSize,1,new Coord(a, b));
		
		for(int i=1; i<lengths.length; i++) {
			int min = Integer.MAX_VALUE;
			int size=0;
			for(int j=i; j>=0; j--) {
				if(lengths[j]<min) {
					min=lengths[j];
				}
			}
			size = min*(i+1);
			if(size>maxSize) {
				maxSize = size;
				r.setWidth(min);
				r.setHeight(i+1);
			}
		}
		
		return r;
	}
	
	/**
	 * Method to increase the size of a rectangle first upwards and then leftwards by increasing it into
	 * the valid zones defined by the parameter zones.
	 * 
	 * @param textRect - the initial rectangle to expand.
	 * @param zones - the list of valid zones to expand into.
	 * @return - the initial rectangle expanded as far as possible first upwards and then leftwards into
	 * the valid zones described by zones.
	 */
	public static Rect expandRect(Rect textRect, List<Zone> zones) {
		
		Rect r = new Rect(textRect);
		
		int maxX=r.getLocation().getX()+r.getWidth(); 
		int maxY=r.getLocation().getY()+r.getHeight();

		int windowSizeX =  zones.get(0).getZoneAtt().getWidth();
		int windowSizeY = zones.get(0).getZoneAtt().getHeight();
		int spacingX = windowSizeX/2;
		int spacingY = windowSizeY/2;
		
		byte[][] zoneGrid = new byte[maxX/spacingX][maxY/spacingY];
		
		for(Zone elem : zones) {
			int xval = elem.getZoneAtt().getLocation().getX();
			int yval = elem.getZoneAtt().getLocation().getY();
			if((xval <= r.getLocation().getX()+r.getWidth()-windowSizeX) 
					&& (yval <= r.getLocation().getY()+r.getHeight()-windowSizeY)) {
				xval = xval/spacingX;
				yval = yval/spacingY;
				zoneGrid[xval][yval] = 1;
				zoneGrid[xval+1][yval] = 1;
				zoneGrid[xval][yval+1] = 1;
				zoneGrid[xval+1][yval+1] = 1;
			}
		}
		
		int increaseHeight = 0;
		for(int i=(r.getLocation().getY()-spacingY)/spacingY; i>=0; i--) {
			int count=0;
			for(int j=r.getLocation().getX()/spacingX; j<(r.getLocation().getX()+r.getWidth())/spacingX; j++) {
				if(zoneGrid[j][i]==1) {
					count++;
				}
			}
			if(count==r.getWidth()/spacingX) {
				increaseHeight++;
			} else {
				break;
			}
		}
		increaseHeight *= spacingY;
		
		r.setLocation(new Coord(r.getLocation().getX(), r.getLocation().getY()-increaseHeight));
		r.setHeight(r.getHeight()+increaseHeight);
		
		
		int increaseWidth = 0;
		for(int i=(r.getLocation().getX()-spacingX)/spacingX; i>=0; i--) {
			int count=0;
			for(int j=r.getLocation().getY()/spacingY; j<(r.getLocation().getY()+r.getHeight())/spacingY; j++) {
				if(zoneGrid[i][j]==1) {
					count++;
				}
			}
			if(count==r.getHeight()/spacingY) {
				increaseWidth++;
			} else {
				break;
			}
		}
		increaseWidth *= spacingX;
		
		r.setLocation(new Coord(r.getLocation().getX()-increaseWidth, r.getLocation().getY()));
		r.setWidth(r.getWidth()+increaseWidth);
		
		return r;
	}
	
	
}
