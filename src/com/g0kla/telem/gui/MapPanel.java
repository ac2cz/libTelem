package com.g0kla.telem.gui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * FOX 1 Telemetry Decoder
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2015 amsat.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Similar to an ImagePanel on it, where we have a map on a panel. It changes size when the panel is 
 * resized, but ina fixed way in line with the rezie of the parent graph
 * 
 *
 */
public class MapPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
    protected BufferedImage image;

    public MapPanel() {
    	super();
    }
    
    public MapPanel(String filePath) throws IOException {
       setImage(filePath);
    }

    private void setImage( String filePath) throws IOException {            
            image = ImageIO.read(new File(filePath));
    }

    public void setBufferedImage( BufferedImage img) {
    	image = img;
    	this.repaint();
    }

	protected void paintMap(Graphics g, int x, int y, int graphHeight, int graphWidth) {
    	if (image != null) {
    		BufferedImage display = image;
    		if (graphHeight < image.getHeight() || graphWidth < image.getWidth()) {
    			double ratio = (double)graphHeight/(double)image.getHeight();
    			if (image.getWidth() * ratio > graphWidth)
    				ratio = (double)graphWidth/(double)image.getWidth();
    			if (ratio > 0)
    				display = scale(image, ratio);
    			//Log.println("RATIO:"+ ratio);
    		}
    		g.drawImage(display, x, y, null); // see javadoc for more info on the parameters
    	}
    }
	
	/**
	 * Utility function to scale an image
	 * @param source
	 * @param ratio
	 * @return
	 */
	public static BufferedImage scale(BufferedImage source,double ratio) {
		  int w = (int) (source.getWidth() * ratio);
		  int h = (int) (source.getHeight() * ratio);
		  BufferedImage bi = getCompatibleImage(w, h);
		  Graphics2D g2d = bi.createGraphics();
		  double xScale = (double) w / source.getWidth();
		  double yScale = (double) h / source.getHeight();
		  AffineTransform at = AffineTransform.getScaleInstance(xScale,yScale);
		  g2d.drawRenderedImage(source, at);
		  g2d.dispose();
		  return bi;
		}

		private static BufferedImage getCompatibleImage(int w, int h) {
		  GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		  GraphicsDevice gd = ge.getDefaultScreenDevice();
		  GraphicsConfiguration gc = gd.getDefaultConfiguration();
		  BufferedImage image = gc.createCompatibleImage(w, h);
		  return image;
		}
}
