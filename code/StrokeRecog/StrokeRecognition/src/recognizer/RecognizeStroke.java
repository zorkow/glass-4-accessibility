package recognizer;

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
 * 
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-08-20
 */
public class RecognizeStroke {

	private static final int SAME_CHAR_THRESH = 5;
	
		
	public static void recognizeCharacters(List<TraceList> characters) {
		
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
				displayableResults += "\n" + (i + 1) + ": '" + character + "' (" + hex + " " + characterName + ") " + characterConfidence + "%";
				finalResults += hex + "(" + characterName + ":" + characterConfidence + ")" + ((i == myrps.size() - 1) ? "" : ", ");
			}
			System.out.println(displayableResults);
			System.out.println(finalResults);

		}
		
		
	}
	
	
	public static List<TraceList> compileCharacters(List<Stroke> strokes) {
		
		List<TraceList> characters = new ArrayList<TraceList>();
		
		byte[] strokeIndicator = new byte[strokes.size()];
		for(int i=0; i<strokeIndicator.length; i++) {
			strokeIndicator[i] = 1;
		}
		
		for(int i=0; i<strokes.size(); i++) {
			TraceList tl = new TraceList();
			Stroke strk = strokes.get(i);
			if(strokeIndicator[i]==1) {
				tl.addTrace(createInkItemFromStroke(strk));
				strokeIndicator[i]=0;
				for(int j=i+1; j<strokes.size(); j++) {
					if(sameCharacter(strk, strokes.get(j))) {
						tl.addTrace(createInkItemFromStroke(strokes.get(j)));
						strokeIndicator[j]=0;
					}
					
				}
				characters.add(tl);
			}
		}
		
		return characters;
	}
	
	
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
	
	public static boolean sameCharacter(Stroke s1, Stroke s2) {
		
		List<SubStroke> lss1 = s1.getPoints();
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
		
		return false;
		
	}
	
}
