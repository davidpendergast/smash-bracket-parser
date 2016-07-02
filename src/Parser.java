import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

public class Parser {
	
	static String nameFile = "names.txt";
	
	static List<Name> names;
	
	static int maxNumPlayers = 100;
	
	static Map<Pair<Name,Name>, Integer> winCount = new HashMap<Pair<Name,Name>, Integer>();
	
	public static void main(String[] args) {
		
		loadNames();
		
		String logFolder = getLogFolder();
		File logDir = new File(logFolder);
		File[] files = logDir.listFiles();
		
		System.out.println("Processing...");
		for (File file : files) {
			System.out.print(file.getName().replaceFirst("\\.txt", ""));
			processLog(file);
			System.out.println();
		}
		System.out.println("Done.\n");
		printNames();
		printWinCounts();	
	}
	
	public static void processLog(File log) {
		List<String> lines = getAllLines(log);
		if(isSmashgg(lines)){
			processSmashggLines(lines);
		} else if (isChallonge(lines)) {
			processChallongeLines(lines);
		} else {
			System.out.print(" -- Failed! Doesn't seem to match challonge or smash.gg format.");
		}
	}
	
	public static boolean isSmashgg(List<String> lines) {
		return lines.size() > 0 && lines.get(0).equals("smash.gg");
	}
	
	public static void processSmashggLines(List<String> lines) {
		/**
		 * We're just looking for 4 sequential lines matching this format:
		 * name1
		 * integer
		 * name2
		 * integer
		 */
		
		for(int i = 0; i < lines.size()-3; i++) {
			String name1 = lines.get(i);
			String numStr1 = lines.get(i+1);
			String name2 = lines.get(i+2);
			String numStr2 = lines.get(i+3);
			
			try {
				int num1 = Integer.parseInt(numStr1);
				int num2 = Integer.parseInt(numStr2);
				
				if (num1 == num2) {
					continue;
				}
				
				String winner = num1 > num2 ? name1 : name2;
				String loser = num1 > num2 ? name2 : name1;
				
				addToCount(winner, loser, 1);
				
			} catch (NumberFormatException e) {
				// just continue
			}
		}
	}
	
	public static boolean isChallonge(List<String> lines) {
		return lines.size() > 0 && lines.get(0).contains("The organizer created this tournament.");
	}
	
	public static void processChallongeLines(List<String> lines) {
		for (String line : lines) {
			if(isWinLine(line)) {
				String winnerStr = getWinner(line);
				String loserStr = getLoser(line);
				
				if (!isBye(loserStr)) {
					addToCount(winnerStr, loserStr, 1);
				}
				
			} else if (isChangeLine(line)) {
				String winnerStr = getChangeWinner(line);
				String loserStr = getChangeLoser(line);
				
				addToCount(loserStr, winnerStr, -1);
				addToCount(winnerStr, loserStr, 1);
			}
		}
	}
	
	public static boolean isBye(String name) {
		return name.matches("Bye") || name.matches("Bye \\d+");
	}
	
	/**
	 * "Apr 09, 21:21 EDT 	Star black Freky (via API) reported a (2-0) win for TRT over 129th at Pound."
	 */
	public static boolean isWinLine(String line) {
		return line.matches(".*reported a.*win.*");
	}
	
	
	public static String getWinner(String line) {
		line = line.replaceFirst(".*win for ", "");
		line = line.replaceFirst(" over.*", "");
		return line;
	}
	
	public static String getLoser(String line) {
		line = line.replaceFirst(".*over ", "");	
		line = line.substring(0, line.lastIndexOf('.'));		
		return line;
	}
	
	/**
	 * "Dhorner40 changed the outcome of Nick vs. Raize to a (0-2) win for Nick."
	 */
	public static boolean isChangeLine(String line) {
		return line.matches(".*changed the outcome.*");
	}
	
	public static String getChangeWinner(String line) {
		line = line.replaceFirst(".*win for ", "");
		return line.substring(0, line.lastIndexOf("."));
	}
	
	public static String getChangeLoser(String line) {
		String player1 = line.replaceFirst(".*outcome of ", "");
		player1 = player1.replaceFirst(" vs.*", "");
		
		String player2 = line.replaceFirst(".* vs\\Q.\\E ", "");
		player2 = player2.replaceFirst(" to a .*", "");
		
		return player1.equals(getChangeWinner(line)) ? player2 : player1;
	}
	
	public static void loadNames() {
		List<String> lines = getAllLines(nameFile);
		names = new ArrayList<Name>();
		
		/**
		 *  lines look like "Ghast ~!~ Nickname1 ~!~ Nickname2 ~!~ Nickname3"
		 */
		for (String line : lines) {
			String[] lineNames = line.split(" ~!~ ");
			Name n = new Name();
			n.name = lineNames[0];
			n.aliases = new ArrayList<String>();
			for(int i = 1; i < lineNames.length; i++) {
				n.aliases.add(lineNames[i]);
			}
			
			names.add(n);
		}
	}
	
	public static List<String> getAllLines(String fileName) {
		return getAllLines(new File(fileName));
	}
	
	public static List<String> getAllLines(File file) {
		List<String> result = new ArrayList<String>();
		try (BufferedReader reader = new BufferedReader(new FileReader(file))){
			String line = reader.readLine();
			while(line != null) {
				result.add(line);
				line = reader.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return result;
	}
	
	public static String getLogFolder() {
		Scanner reader = new Scanner(System.in);
		while(true) {
			System.out.println("Enter log folder name: ");
			String input = reader.next();
			
			File f = new File(input);
			if(f.exists() && f.isDirectory()) {
				reader.close();
				return input;
			} else {
				System.out.println("Directory not found: "+input);
			}
			
		}
	}
	
	public static void addToCount(String winnerStr, String loserStr, int amount) {
		if(find(winnerStr, names) == null) {
			System.out.println("***Adding new name = "+winnerStr+"***");
			names.add(new Name(winnerStr));
		}
		
		if(find(loserStr, names) == null) {
			System.out.println("***Adding new name = "+loserStr+"***");
			names.add(new Name(loserStr));
		}
		
		Name winner = find(winnerStr, names);
		Name loser = find(loserStr, names);
		
		Pair<Name,Name> p = new Pair<Name,Name>(winner, loser);
		
		if (winCount.containsKey(p)) {
			Integer val = winCount.get(p);
			winCount.remove(p);
			winCount.put(p, val + amount);
		} else {
			winCount.put(p, amount);
		}
	}
	
	public static class Pair<X,Y> {
		public X x;
		public Y y;
		
		public Pair(X x, Y y) {
			this.x = x;
			this.y = y;
		}
		
		public boolean equals(Object o) {
			if (o instanceof Pair) {
				return x.equals(((Pair)o).x) &&  y.equals(((Pair)o).y);
			}
			return false;
		}
		
		public int hashCode() {
			return x.hashCode() + y.hashCode()*31;
		}
	}
	
	public static Name find(String str, List<Name> names) {
		for (Name n : names) {
			if (n.equals(str)) {
				return n;
			}
		}
		return null;
	}
	
	public static void printWinCounts() {
		Set<Pair<Name,Name>> allWins = winCount.keySet();
		Set<Name> allParticipants = allWins.stream().map(p -> p.x).collect(Collectors.toSet());
		allParticipants.addAll(allWins.stream().map(p -> p.y).collect(Collectors.toSet())); // grabbing people with no wins
		
		List<Name> players = allParticipants.stream().collect(Collectors.toList());
		Collections.sort(players, (p1, p2) -> numWins(p2) - numWins(p1));
		
		if (maxNumPlayers > 0 && maxNumPlayers < players.size()) {
			players = players.subList(0, maxNumPlayers);
		}
		
		StringBuilder sb = new StringBuilder();
		for (Name name : players) {
			sb.append("\t"+name);
		}
		
		for (Name rowName : players) {
			sb.append("\n"+rowName);
			for (Name colName : players) {
				Pair p = new Pair(rowName, colName);
				int num = winCount.containsKey(p) ? winCount.get(p) : 0;
				sb.append("\t"+num);
			}
		}
		
		System.out.println(sb);
	}
	
	public static int numWins(Name name) {
		int sum = 0;
		for (Pair<Name,Name> p : winCount.keySet()) {	//XXX not good
			if(p.x.equals(name)) {
				sum += winCount.get(p);
			}
		}
		return sum;
	}
	
	public static int numLosses(Name name) {
		int sum = 0;
		for (Pair<Name,Name> p : winCount.keySet()) {	//XXX not good
			if(p.y.equals(name)) {
				sum += winCount.get(p);
			}
		}
		return sum;
	}
	
	public static void printNames() {
		StringBuilder sb = new StringBuilder();
		names.sort((n1, n2) -> n1.name.toUpperCase().compareTo(n2.name.toUpperCase()));
		for(Name n : names) {
			sb.append(n);
			for (String alias : n.aliases) {
				sb.append(" ~!~ "+alias);
			}
			sb.append("\n");
		}
		System.out.println(sb);
	}
}
