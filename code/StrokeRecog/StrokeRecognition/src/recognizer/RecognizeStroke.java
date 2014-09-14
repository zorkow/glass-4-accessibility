package recognizer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import strokeData.Coord;
import strokeData.Stroke;
import strokeData.SubStroke;
import ink.canvas.TraceList;
import ink.elements.items.CanvasInkItem;
import ink.elements.items.TracePoint;
import ink.forms.recognizer.RecClass;
import ink.forms.recognizer.Recognizer;
import ink.recogniser.HighLevelRecognizer;

/**
 * Class which contains static methods related to the recognition of characters defined by Stroke
 * objects.
 * 
 * (Code in method recognizeCharacters is partially adapted from code by Behrang Sabeghi)
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-09-10
 */
public class RecognizeStroke {

	private static final int SAME_CHAR_THRESH = 5;
	
	/**
	 * (Some code in this method is adapted from code by Behrang Sabeghi)
	 * 
	 * The method takes a list of characters as defined by TraceList objects and passes them in 
	 * turn into the on-line handwriting character recognition tool.  The results are printed 
	 * out and also written into a file.
	 * 
	 * @param characters - the list of all characters to recognize.
	 * @param file - the file including path and extension to write the character recognition 
	 * results to.
	 */
	public static void recognizeCharacters(List<TraceList> characters, String file) {
		
		BufferedWriter out = null;
		
		try {
			out = new BufferedWriter(new FileWriter(file));
		
			for(int i=0; i<characters.size(); i++) {
			
				List<Recognizer.Pair> results = HighLevelRecognizer.recognizeWithDistanceAndConfidence(characters.get(i));
				List<RecognizerPair> myrps = RecognizerPair.getRecognizerPair(results);
				String displayableResults = "";
				String finalResults = "";
			
				for (int j=0; j<myrps.size(); j++) {
					RecClass recClass = myrps.get(j).recClass(); // Get the symbol class.
					String character = java.lang.Character.toString((char) Integer.parseInt(recClass.getUnicode().substring(1), 16));
					String characterName = recClass.getName();
					double characterConfidence = Math.round(recClass.getRecConfidence() * 10000) / 100.0;
					String hex = String.format("&#x%04X;", (int) character.charAt(0));
					displayableResults += System.lineSeparator() + (i + 1) + ": '" + character + "' (" + hex + " " + characterName + ") " + characterConfidence + "%";
					finalResults += hex + "(" + characterName + ":" + characterConfidence + ")" + ((i == myrps.size() - 1) ? "" : ", ");
				}
				System.out.println(displayableResults);
				System.out.println(finalResults);
				out.write(displayableResults + System.lineSeparator() + finalResults + System.lineSeparator());
		}
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			if(out!=null) {
				try {
					out.close();
				} catch(IOException e2) {
					e2.printStackTrace();
				}
			}
		}
		
	}
	
	/**
	 * Method which takes a list of strokes which is compiled into a list of characters. Strokes
	 * are compared to check whether they are part of the same character.  Each character is 
	 * defined by a TraceList.
	 * 
	 * @param strokes - the list of all strokes to be compiled into characters.
	 * @return the List of characters defined by TraceLists.
	 */
	public static List<TraceList> compileCharacters(List<Stroke> strokes) {
		
		List<TraceList> characters = new ArrayList<TraceList>();
		
		byte[] strokeIndicator = new byte[strokes.size()];
		for(int i=0; i<strokeIndicator.length; i++) {
			strokeIndicator[i] = 1;
		}
		
		for(int i=0; i<strokes.size(); i++) {
			List<Stroke> sl = new ArrayList<Stroke>();
			Stroke strk = strokes.get(i);
			if(strokeIndicator[i]==1) {
				sl.add(strk);
				strokeIndicator[i]=0;
				for(int j=i+1; j<strokes.size(); j++) {
					if(strokeIndicator[j]!=0 && sameCharacter(sl, strokes.get(j))) {
						sl.add(strokes.get(j));
						strokeIndicator[j]=0;
						j=i;
					}
					
				}
				characters.add(traceListFromStrokeList(sl));
			}
		}
		
		return characters;
	}
	
	/**
	 * Method to convert a Stroke into a CanvasInkItem.
	 * 
	 * @param strk - the Stroke to convert.
	 * @return the CanvasInkItem equivalent of the Stroke.
	 */
	public static CanvasInkItem createInkItemFromStroke(Stroke strk) {
		
		CanvasInkItem item = new CanvasInkItem();
		List<SubStroke> points = strk.getPoints();
		for(int i=0; i<points.size(); i++) {
			Coord c = points.get(i).getStart();
			item.add(new TracePoint(c.getX(), c.getY()));
		}
		Coord c = points.get(points.size()-1).getEnd();
		item.add(new TracePoint(c.getX(), c.getY()));
		return item;
		
	}
	
	/**
	 * Method to check with a Stroke is part of the same character as the character which is
	 * defined by a List of Stroke, sl.  The method checks whether any part of the Stroke s2 is 
	 * within a threshold distance of any part of any of the Strokes within the List of Stroke, 
	 * sl.
	 * 
	 * @param sl - the list of Stroke defining the existing character.
	 * @param s2 - the Stroke to check whether it is part of the existing character.
	 * @return true if the Stroke s2 is part of the existing character, false otherwise.
	 */
	public static boolean sameCharacter(List<Stroke> sl, Stroke s2) {
		
		for(int k=0; k<sl.size(); k++) {
			
			List<SubStroke> lss1 = sl.get(k).getPoints();
			List<SubStroke> lss2 = s2.getPoints();

			for(int i=0; i<lss1.size(); i++) {

				int x1 = (lss1.get(i).getStart().getX() + lss1.get(i).getEnd().getX())/2;
				int y1 = (lss1.get(i).getStart().getY() + lss1.get(i).getEnd().getY())/2;

				for(int j=0; j<lss2.size(); j++) {

					int x2 = (lss2.get(j).getStart().getX() + lss2.get(j).getEnd().getX())/2;
					int y2 = (lss2.get(j).getStart().getY() + lss2.get(j).getEnd().getY())/2;

					double dist = Math.sqrt((Math.pow((x1-x2), 2) + Math.pow((y1-y2), 2)));
					if(dist<SAME_CHAR_THRESH) {
						return true;
					}

				}

			}
		
		}
		
		return false;
		
	}
	
	/**
	 * Method to create a TraceList from a List of Stroke.
	 * 
	 * @param strokes - the list of Stroke to convert to a TraceList.
	 * @return the Tracelist from the converted List of Stroke.
	 */
	public static TraceList traceListFromStrokeList(List<Stroke> strokes) {
		
		TraceList tl = new TraceList();
		for(int i=0; i<strokes.size(); i++) {
			tl.addTrace(createInkItemFromStroke(strokes.get(i)));
		}
		return tl;
		
	}
	
}
