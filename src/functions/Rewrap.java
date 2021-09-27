/*******************************************************************************************
* Copyright (C) 2021 PACIFICO PAUL
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License along
* with this program; if not, write to the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
* 
********************************************************************************************/

package functions;

import java.io.File;
import java.util.Locale;

import application.Ftp;
import application.Settings;
import application.Shutter;
import application.Utils;
import application.VideoPlayer;
import application.Wetransfer;
import library.FFMPEG;
import settings.FunctionUtils;
import settings.InputAndOutput;
import settings.Timecode;

public class Rewrap extends Shutter {
	
	public static void main() {
		
		Thread thread = new Thread(new Runnable(){			
			@Override
			public void run() {
				
				if (scanIsRunning == false)
					FunctionUtils.completed = 0;
				
				lblFilesEnded.setText(FunctionUtils.completedFiles(FunctionUtils.completed));

				for (int i = 0 ; i < liste.getSize() ; i++)
				{
					File file = FunctionUtils.setInputFile(new File(liste.getElementAt(i)));		
					
					if (file == null)
						break;
		            
					try {		
						
						String fileName = file.getName();
						final String extension =  fileName.substring(fileName.lastIndexOf("."));
					
						//Data analyze
						if (FunctionUtils.analyze(file, false) == false)
							continue;
						
						lblCurrentEncoding.setText(fileName);
											
						//Output folder
						String labelOutput = FunctionUtils.setOutputDestination("", file);
						
						//File output name
						String extensionName = "";
												
						//Function cut without re-encoding
						String newExtension = extension;
						String subtitles = "";
						String mapSubtitles = "";
						if (comboFonctions.getSelectedItem().toString().equals(language.getProperty("functionCut")))
						{
							//InOut		
							InputAndOutput.getInputAndOutput();	
							
							//File output name
							extensionName = "_" + Shutter.language.getProperty("cutUpper");
						}
						else
						{
							//Subtitles
							subtitles = setSubtitles();
							
							//Map subtitles
							mapSubtitles = setMapSubtitles();	
							
							//Output extension
							newExtension = comboFilter.getSelectedItem().toString();
						}
						
						if (Settings.btnExtension.isSelected())
							extensionName = Settings.txtExtension.getText();
						
						//Output name
						String fileOutputName =  labelOutput.replace("\\", "/") + "/" + fileName.replace(extension, extensionName + newExtension); 	
														
						//Audio
						String audio = setAudio();
						String audioMapping = setAudioMapping();
						
						//InOut		
						InputAndOutput.getInputAndOutput();
																
		            	//Timecode
						String timecode = Timecode.setTimecode();
						
						//File output
						File fileOut = new File(fileOutputName);				
						if (fileOut.exists())		
						{						
							fileOut = FunctionUtils.fileReplacement(labelOutput, fileName, extension, extensionName + "_", newExtension);
							
							if (fileOut == null)
								continue;						
						}
						
						//Concat mode
						String concat = FunctionUtils.setConcat(file, labelOutput);					
						if (Settings.btnSetBab.isSelected() || VideoPlayer.comboMode.getSelectedItem().toString().equals(language.getProperty("removeMode")) && caseInAndOut.isSelected())
							file = new File(labelOutput.replace("\\", "/") + "/" + fileName.replace(extension, ".txt"));
						else
							concat = " -noaccurate_seek";
										
						//Command
						String cmd = " -avoid_negative_ts make_zero -c:v copy -c:s copy" + audio + timecode + " -map v:0?" + audioMapping + mapSubtitles + " -y ";
						FFMPEG.run(InputAndOutput.inPoint + concat + " -i " + '"' + file.toString() + '"' + subtitles + InputAndOutput.outPoint + cmd + '"'  + fileOut + '"');		
						
						do
						{
							Thread.sleep(100);
						}
						while(FFMPEG.runProcess.isAlive());
						
						if (FFMPEG.error)
						{
							cmd = " -avoid_negative_ts make_zero -c:v copy" + audio + timecode + " -map 0:v:0?" + audioMapping + " -y ";
							FFMPEG.run(InputAndOutput.inPoint + concat + " -i " + '"' + file.toString() + '"' + InputAndOutput.outPoint + cmd + '"'  + fileOut + '"');		
							
							do
							{
								Thread.sleep(100);
							}
							while(FFMPEG.runProcess.isAlive());
						}
						
						if (FFMPEG.saveCode == false && btnStart.getText().equals(Shutter.language.getProperty("btnAddToRender")) == false
						|| FFMPEG.saveCode && VideoPlayer.comboMode.getSelectedItem().toString().equals(language.getProperty("removeMode")) && caseInAndOut.isSelected())
						{
							if (lastActions(fileName, fileOut, labelOutput))
								break;
						}
					
					} catch (InterruptedException e) {
						FFMPEG.error  = true;
					}
				}	
				
				if (btnStart.getText().equals(Shutter.language.getProperty("btnAddToRender")) == false)
					enfOfFunction();
			}
			
		});
		thread.start();
		
    }

	private static String setAudio() {

		if (caseChangeAudioCodec.isSelected())
		{
			if (comboAudioCodec.getSelectedItem().toString().contains("PCM"))
			{
				switch (comboAudioCodec.getSelectedIndex()) 
				{
					case 0 :
						return " -c:a pcm_f32le -ar " + lbl48k.getText() + " -b:a 1536k";
					case 1 :
						return " -c:a pcm_s32le -ar " + lbl48k.getText() + " -b:a 1536k";
					case 2 :
						return " -c:a pcm_s24le -ar " + lbl48k.getText() + " -b:a 1536k";
					case 3 :
						return " -c:a pcm_s16le -ar " + lbl48k.getText() + " -b:a 1536k";
				}
			}
			else if (comboAudioCodec.getSelectedItem().toString().equals("AAC"))
			{
				return " -c:a aac -ar " + lbl48k.getText() + " -b:a " + comboAudioBitrate.getSelectedItem().toString() + "k";
			}
			else if (comboAudioCodec.getSelectedItem().toString().equals("AC3"))
			{
				return " -c:a ac3 -ar " + lbl48k.getText() + " -b:a " + comboAudioBitrate.getSelectedItem().toString() + "k";
			}
			else if (comboAudioCodec.getSelectedItem().toString().equals("OPUS"))
			{
				return " -c:a libopus -ar " + lbl48k.getText() + " -b:a " + comboAudioBitrate.getSelectedItem().toString() + "k";
			}
			else if (comboAudioCodec.getSelectedItem().toString().equals("OGG"))
			{
				return " -c:a libvorbis -ar " + lbl48k.getText() + " -b:a " + comboAudioBitrate.getSelectedItem().toString() + "k";
			}
			else if (comboAudioCodec.getSelectedItem().toString().equals(language.getProperty("noAudio")))
			{
				return " -an";
			}
		}

		return " -c:a copy";		
	}
	
	private static String setAudioMapping() {
	
		String mapping = "";
		if (comboAudio1.getSelectedIndex() == 0
			&& comboAudio2.getSelectedIndex() == 1
			&& comboAudio3.getSelectedIndex() == 2
			&& comboAudio4.getSelectedIndex() == 3
			&& comboAudio5.getSelectedIndex() == 4
			&& comboAudio6.getSelectedIndex() == 5
			&& comboAudio7.getSelectedIndex() == 6
			&& comboAudio8.getSelectedIndex() == 7)
		{
			return " -map a?";	
		}
		else
		{
			if (comboAudio1.getSelectedIndex() != 16)
				mapping += " -map a:" + (comboAudio1.getSelectedIndex()) + "?";
			if (comboAudio2.getSelectedIndex() != 16)
				mapping += " -map a:" + (comboAudio2.getSelectedIndex()) + "?";
			if (comboAudio3.getSelectedIndex() != 16)
				mapping += " -map a:" + (comboAudio3.getSelectedIndex()) + "?";
			if (comboAudio4.getSelectedIndex() != 16)
				mapping += " -map a:" + (comboAudio4.getSelectedIndex()) + "?";
			if (comboAudio5.getSelectedIndex() != 16)
				mapping += " -map a:" + (comboAudio5.getSelectedIndex()) + "?";
			if (comboAudio6.getSelectedIndex() != 16)
				mapping += " -map a:" + (comboAudio6.getSelectedIndex()) + "?";
			if (comboAudio7.getSelectedIndex() != 16)
				mapping += " -map a:" + (comboAudio7.getSelectedIndex()) + "?";
			if (comboAudio8.getSelectedIndex() != 16)
				mapping += " -map a:" + (comboAudio8.getSelectedIndex()) + "?";
		}
		
		return mapping;
	}
	
	private static String setSubtitles() {
		
		if (caseSubtitles.isSelected())
    	{			
    		return InputAndOutput.inPoint + " -i " + '"' +  subtitlesFile.toString() + '"';
    	}
		
		return "";
	}
	
	private static String setMapSubtitles() {
		
		if (caseSubtitles.isSelected())
		{
        	String[] languages = Locale.getISOLanguages();			
			Locale loc = new Locale(languages[comboSubtitles.getSelectedIndex()]);
			        	
        	if (comboFilter.getSelectedItem().toString().equals(".mkv"))
        		return " -map 1:s -c:s srt -metadata:s:s:0 language=" + loc.getISO3Language();
        	else
        		return " -map 1:s -c:s mov_text -metadata:s:s:0 language=" + loc.getISO3Language();      	
		}
		
		return " -map s?";
	}
	
	private static boolean lastActions(String fileName, File fileOut, String output) {
		
		if (FunctionUtils.cleanFunction(fileName, fileOut, output))
			return true;

		//Sending processes
		Utils.sendMail(fileName);
		Wetransfer.addFile(fileOut);
		Ftp.sendToFtp(fileOut);
		Utils.copyFile(fileOut);
		
		//Image sequence and merge
		if (caseEnableSequence.isSelected() || Settings.btnSetBab.isSelected())
			return true;
		
		//Watch folder
		if (Shutter.scanIsRunning)
		{
			FunctionUtils.moveScannedFiles(fileName);
			Rewrap.main();
			return true;
		}
		return false;
	}
}
