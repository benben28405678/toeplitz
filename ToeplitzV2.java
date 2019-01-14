import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class ToeplitzV2 extends PApplet {

int stage = 0;

int index = 0;
int[] setX = new int[600];
int[] setY = new int[600];

int index2 = 0;
int[] curveX = new int[9999];
int[] curveY = new int[9999];

int[] squareX = new int[199];
int[] squareY = new int[199];
int squareIndex = 0;

boolean squareFound = false;

boolean touchedOnce = false;

float progressToDisplay = 0;

//short[][][] seg = new short[99*99][10][20];
int[] seg0 = new int[40], seg45 = new int[40], seg90 = new int[40];
int segIndex1 = 0, segIndex2 = 0, segIndex3 = 0;

float subIndex = 0.0f;

boolean close = false;
boolean kindaClose = false;

int fadeTimer = 0;
float shownStrength = 0;

int tick;
float ttick;

float correlation = 0;

int totalSquaresFound = 0;

public void setup() {
  //fullScreen();
  
  println("User Settings Chosen:");
  println("Tolerance: " + tolerance);
  println("Suppress Almost Warnings: " + suppressAlmostMessages);
  println("--");
}

public void draw() {
  background(255);
  fill(0);
  textAlign(CENTER);
  ttick += 0.1f;
  
  if(stage == 0) {
    text("Click and hold to begin drawing a curve.", width/2, 25);
    if(mousePressed) {
      stage = 1;
    }
  }
  
  if(stage == 1) {
    if(index >= 400) {
      fill(255, 0, 0);
      if(index >= 595) {
        exit();
      }
    }
    
    text("Release the mouse to finish.\nTime Remaining: " + floor(10 - index/60), width/2, 25);
    
    setX[index] = mouseX;
    setY[index] = mouseY;
    index++;
    
    drawCurve();
    
    if(!mousePressed) {
      setX[index] = setX[0];
      setY[index] = setY[0];
      index++;
      
      generatePointSet();
      
      stage = 2;
    }
  }
  
  if(stage == 2) {
    drawCurve();
    
    touchedOnce = false;
    
    rayAndScan();
    
    if(!touchedOnce) {
      //subIndex += PI/10;
    }
    
    if(tick >= index2-10) {
      println();
      println("---------------------");
      println("FINISHED!");
      println("---------------------");
      println("Total Squares Found: " + totalSquaresFound);
      println("Run Time: " + floor(millis() / 1000 / 60) + " minutes");
      println("Base Points Searched: " + index2);
      
      exit();
    }
    
    
    if(progressToDisplay < floor(1000 * PApplet.parseFloat(tick) / index2)/10.0f) {
      progressToDisplay += 0.1f;
    }
    
    text("Finding squares...\n" + floor(10*progressToDisplay)/10.0f + "% complete", width/2, 25);
    noFill();
    strokeWeight(1);
    stroke(0);
    rect(width/2 - 75, 45, 150, 5, 2, 2, 2, 2);
    fill(0);
    rect(width/2 - 75, 45, progressToDisplay * 1.5f, 5, 2, 2, 2, 2);
    textAlign(RIGHT);
    text("Squares Found: " + totalSquaresFound, width - 10, 25);
    textAlign(LEFT);
    if(close) text("Square Possibility: STRONG", 10, 25);
    else if(kindaClose) text("Square Possibility: FAIR", 10, 25);
    else text("Square Possibility: WEAK", 10, 25);
    textAlign(CENTER);
    if(get(mouseX, mouseY) == -1 || mouseY < 50) {
      text("Mouse isn't touching curve.", width/2, height - 25);
    } else {
      text("Mouse is touching curve.", width/2, height - 25);
    }
    
    fill(255, 0, 255);
    noStroke();
    ellipse(curveX[tick], curveY[tick], 5, 5);
    
    if(fadeTimer > 0) {
      noFill();
      strokeWeight(1);
      stroke(0, fadeTimer);
      rect(10, 30, 100, 5);
      fill(0, fadeTimer);
      if(squareFound) {
        rect(10, 30, 100, 5);
      } else {
        rect(10, 30, constrain(shownStrength, 0, 80), 5);
      }
      squareFound = false;
      
      fadeTimer -= 5;
      if(shownStrength > 0) {
        shownStrength -= 2;
      } else {
        shownStrength = 0;
      }
    }
    
    if(squareIndex > 0) {
      for(int i = 0; i < squareIndex; i += 4) {
        noFill();
        stroke(i*10, 0, 255, 75 + 20 * sin(ttick));
        strokeWeight(1);
        quad(squareX[i], squareY[i], squareX[i+1], squareY[i+1], squareX[i+2], squareY[i+2], squareX[i+3], squareY[i+3]);
        println();
        strokeWeight(1);
      }
    }
  }
  
  if(subIndex >= 2*PI) {
    tick += curveX.length / 1000;
    subIndex = 0;
    segIndex1 = 0; segIndex2 = 0; segIndex3 = 0;
  } else {
    if(close) {
      subIndex += PI/500;
    } else if(kindaClose) {
      subIndex += PI/300;
    } else {
      subIndex += PI/50;
    }
    segIndex1 = 0; segIndex2 = 0; segIndex3 = 0;
  }
  if(tick > index2) {
    tick = 0;
  }
  
}

public void rayAndScan() {
  close = false; kindaClose = false;
  // 0 degrees
  for(int i = 10; i < 1000; i++) {
    int num = get(floor(curveX[tick] + i*cos(subIndex)), floor(curveY[tick] + i*sin(subIndex)));
    if(num <= -121505) {
      stroke(255, 0, 0);

      seg0[segIndex1] = floor( dist(curveX[tick] + i*cos(subIndex), curveY[tick] + i*sin(subIndex), curveX[tick], curveY[tick]) );
      
      strokeWeight(constrain(seg0[segIndex1]/10, 0, 10));
      touchedOnce = true;
      stroke(255, 0, 0);
      point(PApplet.parseInt(curveX[tick] + i*cos(subIndex)), PApplet.parseInt(curveY[tick] + i*sin(subIndex)));
      
      fill(255, 0, 0, 2*seg0[segIndex1] - 50);
      textAlign(CENTER);
      text(seg0[segIndex1], PApplet.parseInt(curveX[tick] + i*cos(subIndex)), PApplet.parseInt(curveY[tick] + i*sin(subIndex)) - 20);
      
      if(segIndex1 < 39) segIndex1++;
    } else {
      seg0[segIndex1] = 0;
    }
    
    strokeWeight(1);
    stroke(255, 200, 0);
    point(curveX[tick] + i*cos(subIndex), curveY[tick] + i*sin(subIndex));
  }
  
  // 45 degress
  for(int i = 10; i < 1000; i++) {
    int num = get(floor(curveX[tick] + i*cos(subIndex + PI/4)), floor(curveY[tick] + i*sin(subIndex + PI/4)));
    if(num <= -121505) {
      
      seg45[segIndex2] = floor( dist(curveX[tick] + i*cos(subIndex + PI/4), curveY[tick] + i*sin(subIndex + PI/4), curveX[tick], curveY[tick]) );
      
      stroke(255, 0, 0);
      strokeWeight(constrain(seg45[segIndex2]/10, 0, 10));
      touchedOnce = true;
      stroke(255, 0, 0);
      point(PApplet.parseInt(curveX[tick] + i*cos(subIndex + PI/4)), PApplet.parseInt(curveY[tick] + i*sin(subIndex + PI/4)));
      
      fill(255, 0, 0, 2*seg45[segIndex2] - 50);
      textAlign(CENTER);
      text(seg45[segIndex2], PApplet.parseInt(curveX[tick] + i*cos(subIndex + PI/4)), PApplet.parseInt(curveY[tick] + i*sin(subIndex + PI/4)) - 25);
      
      fill(255, 0, 0, 2*seg45[segIndex2] - 50);
      text(PApplet.parseInt(seg45[segIndex2] / sqrt(2)), PApplet.parseInt(curveX[tick] + i*cos(subIndex + PI/4)), PApplet.parseInt(curveY[tick] + i*sin(subIndex + PI/4)) - 10);
      
      if(segIndex2 < 39) segIndex2++;
    } else {
      seg45[segIndex2] = 0;
    }
    
    strokeWeight(1);
    stroke(255, 200, 0);
    point(curveX[tick] + i*cos(subIndex + PI/4), curveY[tick] + i*sin(subIndex + PI/4));
  }
  
  // 90 degress
  for(int i = 10; i < 1000; i++) {
    int num = get(floor(curveX[tick] + i*cos(subIndex + PI/2)), floor(curveY[tick] + i*sin(subIndex + PI/2)));
    if(num <= -121505) {
      
      seg90[segIndex3] = floor( dist(curveX[tick] + i*cos(subIndex + PI/2), curveY[tick] + i*sin(subIndex + PI/2), curveX[tick], curveY[tick]) );
      
      stroke(255, 0, 0);
      strokeWeight(constrain(seg90[segIndex3]/10, 0, 10));
      touchedOnce = true;
      stroke(255, 0, 0);
      point(PApplet.parseInt(curveX[tick] + i*cos(subIndex + PI/2)), PApplet.parseInt(curveY[tick] + i*sin(subIndex + PI/2)));
      
      fill(255, 0, 0, 2*seg90[segIndex3] - 50);
      textAlign(CENTER);
      text(seg90[segIndex3], PApplet.parseInt(curveX[tick] + i*cos(subIndex + PI/2)), PApplet.parseInt(curveY[tick] + i*sin(subIndex + PI/2)) - 20);
      
      if(segIndex3 < 39) segIndex3++;
    } else {
      seg90[segIndex3] = 0;
    }
    
    strokeWeight(1);
    stroke(255, 200, 0);
    point(curveX[tick] + i*cos(subIndex + PI/2), curveY[tick] + i*sin(subIndex + PI/2));
  }
  fill(0);
  
  for(int i = 0; i < 1; i++) {
    if(abs(seg0[i] - seg90[i]) < 20 && seg0[i] > 20 && seg45[i] > 20 && seg90[i] > 20 && seg0[i] != seg45[i] && seg90[i] != seg45[i]) {
      close = true;
    }
    if(abs(seg0[i] - seg90[i]) < 60 && seg0[i] > 60 && seg45[i] > 60 && seg90[i] > 60 && seg0[i] != seg45[i] && seg90[i] != seg45[i]) {
      noStroke();
      fill(0);
      correlation = norm(60 - abs(seg0[i] - seg90[i]), 0, 60) * norm(60 - abs(seg0[i] - seg45[i]/sqrt(2)) * norm(60 - abs(seg90[i] - seg45[i]/sqrt(2)), 0, 60), 0, 60);
      if(seg0[i] <= floor(seg45[i] / sqrt(2)) + 2 && seg0[i] >= floor(seg45[i] / sqrt(2)) - 2) correlation = 1;
      fadeTimer = 255;
      if(constrain(correlation*100, 0, 100) > shownStrength) {
        shownStrength = constrain(correlation*100, 0, 100);
      }
      kindaClose = true;
    }
    
    if(seg0[i] <= seg90[i] + tolerance && seg0[i] >= seg90[i] - tolerance  && seg0[i] > 20 && seg45[i] > 20 && seg90[i] > 20 && seg0[i] != seg45[i] && seg90[i] != seg45[i]) {
      if(!suppressAlmostMessages) println("ALMOST!: " + seg0[i] + " " + seg45[i] + " " + seg90[i] + "\t45deg / sqrt(2): " + seg0[i] + " " + floor(seg45[i]/sqrt(2)) + " " + seg90[i]);
      correlation = 0;
      if(seg0[i] <= floor(seg45[i] / sqrt(2)) + tolerance && seg0[i] >= floor(seg45[i] / sqrt(2)) - tolerance) {
        
        println("SUCCESS! Original: " + seg0[i] + " " + seg45[i] + " " + seg90[i] + "\t45deg / sqrt(2): " + seg0[i] + " " + floor(seg45[i]/sqrt(2)) + " " + seg90[i]);
        totalSquaresFound++;
        squareFound = true;
        
        squareX[squareIndex] = curveX[tick]; squareY[squareIndex] = curveY[tick]; squareIndex++;
        squareX[squareIndex] = floor(curveX[tick] + seg0[i]*cos(subIndex)); squareY[squareIndex] = floor(curveY[tick] + seg0[i]*sin(subIndex)); squareIndex++;
        squareX[squareIndex] = floor(curveX[tick] + seg45[i]*cos(subIndex + PI/4)); squareY[squareIndex] = floor(curveY[tick] + seg45[i]*sin(subIndex + PI/4)); squareIndex++;
        squareX[squareIndex] = floor(curveX[tick] + seg90[i]*cos(subIndex + PI/2)); squareY[squareIndex] = floor(curveY[tick] + seg90[i]*sin(subIndex + PI/2)); squareIndex++;
        
        if(squaresToImages) saveFrame("SquaresFound/Square_" + totalSquaresFound);
        pauseFrame(2);
      }
    }
  }
}

public void pauseFrame(float delay) {
  delay(PApplet.parseInt(delay * 1000));
}

public void drawCurve() {
  fill(0);
  
  for(int i = 1; i < index; i++) {
    stroke(0);
    line(setX[i-1], setY[i-1], setX[i], setY[i]);
  }
}

public void generatePointSet() {
  for(int i = 15; i < width-15; i++) {
    for(int j = 50; j < height-15; j++) {
      if(get(i,j) != -1) {
        curveX[index2] = i;
        curveY[index2] = j;
        index2++;
      }
    }
  }
}
// USER SETTINGS

// Set this to 0 for the most accurate results.
private int tolerance = 2;

// Suppress ALMOST! console messages
private boolean suppressAlmostMessages = true;

// Save squares found as image files
private boolean squaresToImages = true;
  public void settings() {  size(1000, 600); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "ToeplitzV2" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
