package model;
import java.util.Locale; 
import javax.speech.Central; 
import javax.speech.synthesis.Synthesizer; 
import javax.speech.synthesis.SynthesizerModeDesc;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Port;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.result.WordResult;

public class SpeechRecognizerMain {
	
	private LiveSpeechRecognizer recognizer;
	
	private Logger logger = Logger.getLogger(getClass().getName());

	private String speechRecognitionResult;
	
	private boolean ignoreSpeechRecognitionResults = false;
	

	private boolean speechRecognizerThreadRunning = false;
	
	private boolean resourcesThreadRunning;
	
	private ExecutorService eventsExecutorService = Executors.newFixedThreadPool(2);
	
	public SpeechRecognizerMain() throws IOException {
		
		logger.log(Level.INFO, "Loading Speech Recognizer...\n");
		
		Configuration configuration = new Configuration();
		
		configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
		configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
		
		configuration.setGrammarPath("resource:/grammars");
		configuration.setGrammarName("grammar");
		configuration.setUseGrammar(true);
		
		try {
			recognizer = new LiveSpeechRecognizer(configuration);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		
		startResourcesThread();
		startSpeechRecognition();
	}
	
	public void speak(String x){
		try { 
            System.setProperty( 
                "freetts.voices", 
                "com.sun.speech.freetts.en.us"
                    + ".cmu_us_kal.KevinVoiceDirectory"); 
  
            Central.registerEngineCentral( 
                "com.sun.speech.freetts"
                + ".jsapi.FreeTTSEngineCentral"); 
  
            Synthesizer synthesizer 
                = Central.createSynthesizer( 
                    new SynthesizerModeDesc(Locale.US));
            System.out.println("test");
  
            synthesizer.allocate(); 
  
            synthesizer.resume(); 
  
            synthesizer.speakPlainText( 
                x, null); 
            synthesizer.waitEngineState( 
                Synthesizer.QUEUE_EMPTY); 
  
            synthesizer.deallocate(); 
        }catch (Exception e) { 
            e.printStackTrace(); 
        }
	}
	
	public synchronized void startSpeechRecognition() {
		
		if (speechRecognizerThreadRunning)
			logger.log(Level.INFO, "Speech Recognition Thread already running...\n");
		else
			eventsExecutorService.submit(() -> {
				
				speechRecognizerThreadRunning = true;
				ignoreSpeechRecognitionResults = false;
				
				recognizer.startRecognition(true);
					
				logger.log(Level.INFO, "You can start to speak...\n");
				
				try {
					while (speechRecognizerThreadRunning) {
						SpeechResult speechResult = recognizer.getResult();
						
						if (!ignoreSpeechRecognitionResults) {
							
							if (speechResult == null)
								logger.log(Level.INFO, "I can't understand what you said.\n");
							else {
								
								speechRecognitionResult = speechResult.getHypothesis();
								
								System.out.println("You said: [" + speechRecognitionResult + "]\n");
								if(speechRecognitionResult.compareTo("open google") == 0){
									try {
										Desktop desktop = java.awt.Desktop.getDesktop();
										URI oURL = new URI("http://www.google.com");
//										speak("Opening Google");
										desktop.browse(oURL);
									} catch (Exception e) {
										e.printStackTrace();
									}									
								}else if(speechRecognitionResult.compareTo("open youtube") == 0){
									try {
										Desktop desktop = java.awt.Desktop.getDesktop();
										URI oURL = new URI("http://www.youtube.com");
										desktop.browse(oURL);
									} catch (Exception e) {
										e.printStackTrace();
									}									
								}else if(speechRecognitionResult.compareTo("open facebook") == 0){
									try {
										Desktop desktop = java.awt.Desktop.getDesktop();
										URI oURL = new URI("http://www.facebook.com");
										desktop.browse(oURL);
									} catch (Exception e) {
										e.printStackTrace();
									}									
								}else if(speechRecognitionResult.compareTo("open maps") == 0){
									try {
										Desktop desktop = java.awt.Desktop.getDesktop();
										URI oURL = new URI("http://www.googlemaps.com");
										desktop.browse(oURL);
									} catch (Exception e) {
										e.printStackTrace();
									}									
								}else if(speechRecognitionResult.compareTo("open notepad") == 0){
									Runtime rs = Runtime.getRuntime();
									try{
										rs.exec("notepad");
									}catch(IOException e){
										
									}
								}else if(speechRecognitionResult.compareTo("show date") == 0){
									   DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
									   LocalDateTime now = LocalDateTime.now();  
									   System.out.println(dtf.format(now));  
								}else if(speechRecognitionResult.compareTo("open binusmaya") == 0){
									try {
										Desktop desktop = java.awt.Desktop.getDesktop();
										URI oURL = new URI("http://www.binusmaya.ac.id");
										desktop.browse(oURL);
									} catch (Exception e) {
										e.printStackTrace();
									}	
								}
								
								makeDecision(speechRecognitionResult, speechResult.getWords());
								
							}
						} else
							logger.log(Level.INFO, "Ingoring Speech Recognition Results...");
						
					}
				} catch (Exception ex) {
					logger.log(Level.WARNING, null, ex);
					speechRecognizerThreadRunning = false;
				}
				
				logger.log(Level.INFO, "SpeechThread has exited...");
				
			});
	}
	
	public synchronized void stopIgnoreSpeechRecognitionResults() {
		
		ignoreSpeechRecognitionResults = false;
	}
	
	public synchronized void ignoreSpeechRecognitionResults() {
		
		ignoreSpeechRecognitionResults = true;
		
	}
	

	public void startResourcesThread() {
		
		if (resourcesThreadRunning)
			logger.log(Level.INFO, "Resources Thread already running...\n");
		else
			eventsExecutorService.submit(() -> {
				try {
					
					resourcesThreadRunning = true;
					
					while (true) {
						
						if (!AudioSystem.isLineSupported(Port.Info.MICROPHONE))
							logger.log(Level.INFO, "Microphone is not available.\n");
						
						Thread.sleep(350);
					}
					
				} catch (InterruptedException ex) {
					logger.log(Level.WARNING, null, ex);
					resourcesThreadRunning = false;
				}
			});
	}

	public void makeDecision(String speech , List<WordResult> speechWords) {
		
		System.out.println(speech);
		
	}
	
	public boolean getIgnoreSpeechRecognitionResults() {
		return ignoreSpeechRecognitionResults;
	}
	
	public boolean getSpeechRecognizerThreadRunning() {
		return speechRecognizerThreadRunning;
	}
	
	
	public static void main(String[] args) throws IOException {
		new SpeechRecognizerMain();
	}
}
