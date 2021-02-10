/*
    Tunnista.java for DLI 2021 (derived from the one used in DMT 2019)
    Copyright (C) 2019, 2020, 2021 Tommi Jauhiainen
    Copyright (C) 2021 University of Helsinki
 
    The creation and publication of this software has been partly funded by The Finnish Research Impact Foundation.
 
    If you use this software in context of academic publishing, please consider referring to the following publication:
    @inproceedings{jauhiainen2019mandarin,
        title={{Discriminating between Mandarin Chinese and Swiss-German varieties using adaptive language models}},
        author={Jauhiainen, Tommi and Jauhiainen, Heidi and Lind\'{e}n, Krister},
        booktitle={Proceedings of the 6th Workshop on NLP for Similar Languages, Varieties and Dialects (VarDial 2019)},
        address = {Minneapolis, Minnesota},
        pages = {178--187},
        year={2019}
    }

    Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

import java.io.*;
import java.util.*;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.lang.Math.*;

class Tunnista {

// global table holding the language models for all the languages

	private static Table<String, String, Double> gramDictCap;
	private static Table<String, Integer, Double> typeAmounts;

	public static void main(String[] args) {
	
		String trainFile = args[0];
		String testFile = args[1];
	
		gramDictCap = HashBasedTable.create();
		typeAmounts = HashBasedTable.create();
		
		List<String> languageList = new ArrayList<String>();
		
		int minCharNgram = 2;
		int maxCharNgram = 6;
//		String method = "simple";
//		String method = "sumrelfreq";
		String method = "prodrelfreq";
//		String method = "ensemble";

//		System.out.println("Next: creating models.");
		
		languageList = createModels(trainFile,minCharNgram,maxCharNgram);
		
//		System.out.println("Models created.");

		File file = new File(testFile);
		
		int x = minCharNgram;
		int y = maxCharNgram;
		double penaltymodifier = 2.15;
		
		if (method.equals("prodrelfreq")) {
			while (x <= maxCharNgram) {
				y = maxCharNgram;
				while (y >= x) {
					double smooth = penaltymodifier;
					while (smooth > 2.14) {
						evaluateFile(file,languageList,x,y,method,smooth);
						smooth = smooth - 0.01;
					}
					y--;
				}
				x++;
			}
		}
		else if (!method.equals("ensemble")) {
			while (x <= maxCharNgram) {
				y = maxCharNgram;
				while (y >= x) {
					evaluateFile(file,languageList,x,y,method,penaltymodifier);
					y--;
				}
				x++;
			}
		}
		else {
			while (x <= maxCharNgram) {
				y = maxCharNgram;
				while (y >= x) {
					evaluateFile(file,languageList,x,y,method,penaltymodifier);
					y--;
				}
				x++;
			}
		}
	}

	private static void evaluateFile(File file, List<String> languageList, int minCharNgram, int maxCharNgram, String method, double penaltymodifier) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			
			String line = "";
			
			Map<String, Integer> langCorrect;
			Map<String, Integer> langWrong;
			Map<String, Integer> langShouldBe;

			langCorrect = new LinkedHashMap<String, Integer>();
			langWrong = new LinkedHashMap<String, Integer>();
			langShouldBe = new LinkedHashMap<String, Integer>();

			ListIterator languageiterator = languageList.listIterator();
			while(languageiterator.hasNext()) {
				Object element = languageiterator.next();
				String language = (String) element;
				langShouldBe.put(language,0);
				langCorrect.put(language,0);
				langWrong.put(language,0);
			}
			
			langWrong.put("xxx",0);

			float correct = 0;
			float wrong = 0;
			float total = 0;
			
			int totallinenumber = 0;
			
			while ((line = reader.readLine()) != null) {
				String mysterytext = line;
				String correctlanguage = line;
								
				mysterytext = mysterytext.replaceAll("\t.*", "");

				correctlanguage = correctlanguage.replaceAll(".*\t", "");
				correctlanguage = correctlanguage.replaceAll("\n", "");
				correctlanguage = correctlanguage.replaceAll("\\W", "");

// Using only alphabetical characters
				mysterytext = mysterytext.replaceAll("[^\\p{L}\\p{M}′'’´ʹािीुूृेैोौंँः् া ি ী ু ূ ৃ ে ৈ ো ৌ।্্্я̄\\u07A6\\u07A7\\u07A8\\u07A9\\u07AA\\u07AB\\u07AC\\u07AD\\u07AE\\u07AF\\u07B0\\u0A81\\u0A82\\u0A83\\u0ABC\\u0ABD\\u0ABE\\u0ABF\\u0AC0\\u0AC1\\u0AC2\\u0AC3\\u0AC4\\u0AC5\\u0AC6\\u0AC7\\u0AC8\\u0AC9\\u0ACA\\u0ACB\\u0ACC\\u0ACD\\u0AD0\\u0AE0\\u0AE1\\u0AE2\\u0AE3\\u0AE4\\u0AE5\\u0AE6\\u0AE7\\u0AE8\\u0AE9\\u0AEA\\u0AEB\\u0AEC\\u0AED\\u0AEE\\u0AEF\\u0AF0\\u0AF1]", " ");
				mysterytext = mysterytext.replaceAll("  *", " ");
				mysterytext = mysterytext.replaceAll("^ ", "");
				mysterytext = mysterytext.replaceAll(" $", "");
				mysterytext = mysterytext.toLowerCase();
				
				String identifiedLanguage = "xxx";
				
				if (method.equals("simple")) {
					identifiedLanguage = identifyTextSimpleScoring(mysterytext,languageList,minCharNgram,maxCharNgram);
				}
				if (method.equals("sumrelfreq")) {
					identifiedLanguage = identifyTextSumRelFreq(mysterytext,languageList,minCharNgram,maxCharNgram);
				}
				if (method.equals("prodrelfreq")) {
					identifiedLanguage = identifyTextProdRelFreq(mysterytext,languageList,minCharNgram,maxCharNgram,penaltymodifier);
				}
				if (method.equals("ensemble")) {
					String simplelanguage = identifyTextSimpleScoring(mysterytext,languageList,1,6);
					// 0.90149796
					String sumrellanguage = identifyTextSumRelFreq(mysterytext,languageList,5,15);
					// 0.8247106
					String prodrellanguage = identifyTextProdRelFreq(mysterytext,languageList,1,13,1.3);
					// 0.9284998
					if (!sumrellanguage.equals(simplelanguage)) {
						identifiedLanguage = prodrellanguage;
					}
					else {
						identifiedLanguage = sumrellanguage;
					}
					// 0.915
				}


				langShouldBe.put(correctlanguage,langShouldBe.get(correctlanguage)+1);

				total++;
				if (identifiedLanguage.equals(correctlanguage)) {
					correct++;
					langCorrect.put(identifiedLanguage,langCorrect.get(identifiedLanguage)+1);
				}
				else {
					wrong++;
//					System.out.println(identifiedLanguage);
//					System.out.println(mysterytext + "\t" + correctlanguage + "\t" +  identifiedLanguage);
					langWrong.put(identifiedLanguage,langWrong.get(identifiedLanguage)+1);
				}
				totallinenumber++;

//				System.out.println(identifiedLanguage);
			}

			Float sumFscore = (float)0;
			Float weightedF1Score = (float)0;

			languageiterator = languageList.listIterator();
			int langamount = 0;
			while(languageiterator.hasNext()) {
				langamount++;
				Object element = languageiterator.next();
				String language = (String) element;
//				System.out.println(language);
				Float precision = (float)langCorrect.get(language) / (float)(langCorrect.get(language) + (float)langWrong.get(language));
				Float recall = (float)langCorrect.get(language) / (float)langShouldBe.get(language);
				Float f1score = 2*(precision*recall/(precision+recall));
				sumFscore = sumFscore + f1score;
				weightedF1Score = weightedF1Score + f1score * (float)langShouldBe.get(language) / (float)totallinenumber;
// Use this to print out language specific scores
//				System.out.println("minCharNgram: " + minCharNgram + "\tmaxCharNgram: " + maxCharNgram + "\tIndividual - Language: " + language + "\tRecall: " + recall + "\tPrecision: " + precision + "\tF1-score: " + f1score);
			}
			
			Float macroF1Score = sumFscore / (float)langamount;
		
			Float totalPrecision = correct / (correct + wrong);
			Float totalRecall = correct / (correct + wrong);
			
			Float microF1Score = 2*(totalPrecision*totalRecall/(totalPrecision+totalRecall));
			
//			System.out.println(penaltymodifier + " " + total + " " + correct + " " + wrong + " " + 100.0/total*correct + " " + microF1Score + macroF1Score);
			System.out.println("Penaltymodifier: " + penaltymodifier + "Method: " + method + "\tminCharNgram: " + minCharNgram + "\tmaxCharNgram: " + maxCharNgram + "\tTotal - MacroF1: " + macroF1Score + "\tRecall: " + 100.0/total*correct + "\tMicroF1: " + microF1Score + "\tWeighted F1: " + weightedF1Score);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
			}
		}
	}

// identifyText

	private static String identifyTextSimpleScoring(String mysteryText, List<String> languageList, int minCharNgram, int maxCharNgram) {
	
		Map<String, Double> languagescores = new HashMap();

		ListIterator languageiterator = languageList.listIterator();
		while(languageiterator.hasNext()) {
			Object element = languageiterator.next();
			String kieli = (String) element;
			languagescores.put(kieli, 0.0);
		}

		int t = maxCharNgram;

		while (t >= minCharNgram) {
			int pituus = mysteryText.length();
			int x = 0;
			if (pituus > (t-1)) {
				while (x < pituus - t + 1) {
					String gram = mysteryText.substring(x,x+t);
					
					languageiterator = languageList.listIterator();
					while(languageiterator.hasNext()) {
						Object element = languageiterator.next();
						String kieli = (String) element;
						if (gramDictCap.contains(gram,kieli)) {
							languagescores.put(kieli, languagescores.get(kieli) + 1);
						}
					}
					x = x + 1;
				}
			}
			t = t -1 ;
		}

		Double winningscore = 0.0;
		String mysterylanguage = "xxx";

		languageiterator = languageList.listIterator();
		while(languageiterator.hasNext()) {
			Object element = languageiterator.next();
			String kieli = (String) element;
//					System.out.println(kieli + " " + languagescores.get(element));
			if (languagescores.get(element) > winningscore) {
				winningscore = languagescores.get(element);
				mysterylanguage = kieli;
			}
		}
		return (mysterylanguage);
	}
	
	private static String identifyTextSumRelFreq(String mysteryText, List<String> languageList, int minCharNgram, int maxCharNgram) {
		
		Map<String, Double> languagescores = new HashMap();

		ListIterator languageiterator = languageList.listIterator();
		while(languageiterator.hasNext()) {
			Object element = languageiterator.next();
			String kieli = (String) element;
			languagescores.put(kieli, 0.0);
		}

		int t = maxCharNgram;

		while (t >= minCharNgram) {
			int pituus = mysteryText.length();
			int x = 0;
			if (pituus > (t-1)) {
				while (x < pituus - t + 1) {
					String gram = mysteryText.substring(x,x+t);
					
					languageiterator = languageList.listIterator();
					while(languageiterator.hasNext()) {
						Object element = languageiterator.next();
						String kieli = (String) element;
						if (gramDictCap.contains(gram,kieli)) {
							languagescores.put(kieli, languagescores.get(kieli) + gramDictCap.get(gram,kieli)/typeAmounts.get(kieli,t));
						}
					}
					x = x + 1;
				}
			}
			t = t -1 ;
		}

		Double winningscore = 0.0;
		String mysterylanguage = "xxx";

		languageiterator = languageList.listIterator();
		while(languageiterator.hasNext()) {
			Object element = languageiterator.next();
			String kieli = (String) element;
			if (languagescores.get(element) > winningscore) {
				winningscore = languagescores.get(element);
				mysterylanguage = kieli;
			}
		}
		return (mysterylanguage);
	}
	
	private static String identifyTextProdRelFreq(String mysteryText, List<String> languageList, int minCharNgram, int maxCharNgram, double penaltymodifier) {
		
		Map<String, Double> languagescores = new HashMap();

		ListIterator languageiterator = languageList.listIterator();
		while(languageiterator.hasNext()) {
			Object element = languageiterator.next();
			String kieli = (String) element;
			languagescores.put(kieli, 0.0);
		}

		int t = maxCharNgram;
		int gramamount = 0;

		while (t >= minCharNgram) {
			int pituus = mysteryText.length();
			int x = 0;
			if (pituus > (t-1)) {
				while (x < pituus - t + 1) {
					String gram = mysteryText.substring(x,x+t);
					gramamount = gramamount + 1;
					languageiterator = languageList.listIterator();
					while(languageiterator.hasNext()) {
						Object element = languageiterator.next();
						String kieli = (String) element;
						if (gramDictCap.contains(gram,kieli)) {
							double probability = -Math.log10(gramDictCap.get(gram,kieli) / (typeAmounts.get(kieli,t)));
							languagescores.put(kieli, languagescores.get(kieli) +probability);
						}
						else {
							double penalty = -Math.log10(1/typeAmounts.get(kieli,t))*penaltymodifier;
							languagescores.put(kieli, languagescores.get(kieli) +penalty);
						}
					}
					x = x + 1;
				}
			}
			t = t -1 ;
		}
		
		languageiterator = languageList.listIterator();
		while(languageiterator.hasNext()) {
			Object element = languageiterator.next();
			String language = (String) element;
			languagescores.put(language, (languagescores.get(language)/gramamount));
		}

		Double winningscore = 1000.0;
		String mysterylanguage = "xxx";

		languageiterator = languageList.listIterator();
		while(languageiterator.hasNext()) {
			Object element = languageiterator.next();
			String kieli = (String) element;
			if (languagescores.get(element) < winningscore) {
				winningscore = languagescores.get(element);
				mysterylanguage = kieli;
			}
		}
		return (mysterylanguage);
	}
	
	private static List createModels(String trainFile, int minCharNgram, int maxCharNgram) {
	
		List<String> languageList = new ArrayList<String>();
	
		File file = new File(trainFile);
		
		int lineNumber = 0;
		int ngramNumber = 0;
		
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			
			String line = "";
			String gram = "";
			
			Double aika = (double)System.currentTimeMillis();
			
			while ((line = reader.readLine()) != null) {
				String text = line;
				String language = line;
				
//				System.out.println("Line:" + lineNumber + " ngrams: " + ngramNumber);
				
				text = text.replaceAll("\t.*", "");
				
// Using only alphabetical characters
				text = text.replaceAll("[^\\p{L}\\p{M}′'’´ʹािीुूृेैोौंँः् া ি ী ু ূ ৃ ে ৈ ো ৌ।্্্я̄\\u07A6\\u07A7\\u07A8\\u07A9\\u07AA\\u07AB\\u07AC\\u07AD\\u07AE\\u07AF\\u07B0\\u0A81\\u0A82\\u0A83\\u0ABC\\u0ABD\\u0ABE\\u0ABF\\u0AC0\\u0AC1\\u0AC2\\u0AC3\\u0AC4\\u0AC5\\u0AC6\\u0AC7\\u0AC8\\u0AC9\\u0ACA\\u0ACB\\u0ACC\\u0ACD\\u0AD0\\u0AE0\\u0AE1\\u0AE2\\u0AE3\\u0AE4\\u0AE5\\u0AE6\\u0AE7\\u0AE8\\u0AE9\\u0AEA\\u0AEB\\u0AEC\\u0AED\\u0AEE\\u0AEF\\u0AF0\\u0AF1]", " ");
				text = text.replaceAll("  *", " ");
				text = text.replaceAll("^ ", "");
				text = text.replaceAll(" $", "");
				text = text.toLowerCase();
				
				int pituus = text.length();
				
				language = language.replaceAll(".*\t", "");
				language = language.replaceAll("\n", "");
				language = language.replaceAll("\\W", "");
				
				if (!languageList.contains(language)) {
					languageList.add(language);
					int x = maxCharNgram;
					while (x >= minCharNgram) {
						typeAmounts.put(language,x,0.0);
						x--;
					}
				}
				
				int t = maxCharNgram;
				
				while (t >= minCharNgram) {
					int x = 0;
					int typeAmountCounter = 0;
					if (pituus > (t-1)) {
						while (x < pituus - t + 1) {
							
							gram = text.substring(x,x+t);
							
							if (gramDictCap.contains(gram,language)) {
								gramDictCap.put(gram, language, gramDictCap.get(gram,language) + 1);
							}
							else {
								gramDictCap.put(gram,language, 1.0);
								ngramNumber++;

							}
							typeAmountCounter++;
							
							x = x + 1;
						}
					}
					typeAmounts.put(language,t,typeAmounts.get(language,t)+typeAmountCounter);
					t = t -1 ;
				}
				lineNumber ++;
			}
			
			Double aika2 = (double)System.currentTimeMillis();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
			}
		}
		return (languageList);
	}
}
