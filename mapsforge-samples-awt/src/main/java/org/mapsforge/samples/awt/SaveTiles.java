/*
 * Copyright 2017 Raymond Wu
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.samples.awt;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.labels.TileBasedLabelStore;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.FixedTileSizeDisplayModel;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This sample demo how to render & save a tile.
 */
public class SaveTiles {

    private static final String HOME = System.getProperty("user.home");
    private static final String SAVE_PATH = "Documents/MyTiles";

    // Your compiled map. 
    private static final File DEFAULT_MAP_PATH = new File(HOME + "/berlin.map");

    // Location you'd like to render.
    // LAT is Y
    private static final double LAT =52.51505;
    // LNG is X
    private static final double LNG = 13.40165;
    private static final byte ZOOM = 16;
    private static final int TILE_SIZE = 256;

    private static List<LatLong> polyLinePoints = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        // TODO Use args for all parameters

        polyLinePoints.add(new LatLong(52.5459, 13.3837));
        polyLinePoints.add(new LatLong(52.5453, 13.3864));
        polyLinePoints.add(new LatLong(52.54665, 13.38733));
        polyLinePoints.add(new LatLong(52.54235, 13.38671));
        LatLong topLeft = getTopLeft();
        LatLong bottomRight = getBottomRight();

        // Load map.
        MapDataStore mapData = new MapFile(DEFAULT_MAP_PATH);

        int firstTileX = MercatorProjection.longitudeToTileX(topLeft.getLongitude(), ZOOM);
        int firstTileY = MercatorProjection.latitudeToTileY(topLeft.getLatitude(), ZOOM);
        int lastTileX = MercatorProjection.longitudeToTileX(bottomRight.getLongitude(), ZOOM);
        // TODO: this is weird, but might need something like this.
        int lastTileY = MercatorProjection.latitudeToTileY(bottomRight.getLatitude(), ZOOM) + 1;


        // Create requirements.
        GraphicFactory gf = AwtGraphicFactory.INSTANCE;
        XmlRenderTheme theme = InternalRenderTheme.OSMARENDER;
        DisplayModel dm = new FixedTileSizeDisplayModel(TILE_SIZE);
        RenderThemeFuture rtf = new RenderThemeFuture(gf, theme, dm);
        File cacheDir = new File(HOME, SAVE_PATH);
        FileSystemTileCache tileCache = new FileSystemTileCache(100, cacheDir, gf, false);
        TileBasedLabelStore tileBasedLabelStore = new TileBasedLabelStore(tileCache.getCapacityFirstLevel());

        // Create renderer.
        DatabaseRenderer renderer = new DatabaseRenderer(mapData, gf, tileCache, tileBasedLabelStore, true, true, null);

        // Create RendererTheme.
        Thread t = new Thread(rtf);
        t.start();

        //Tile tile = new Tile(tx, ty, ZOOM, TILE_SIZE);
        Tile firstTile = new Tile(firstTileX, firstTileY, ZOOM, TILE_SIZE);
        Tile lastTile = new Tile(lastTileX, lastTileY, ZOOM, TILE_SIZE);
        int xDiff = lastTile.tileX - firstTile.tileX;
        int yDiff = lastTile.tileY - firstTile.tileY;

        File[][] images = new File[Math.abs(xDiff) + 1][Math.abs(yDiff)];
        InputStream[][] streams = new InputStream[Math.abs(xDiff) + 1][Math.abs(yDiff)];

        for (int i = 0; i <= Math.abs(xDiff); i++) {
            int x = firstTileX + (xDiff > 0 ? i : -i);
            Tile currentTile = null;
            int j = 0;
            if (yDiff > 0) {
                for (j = 0; j < Math.abs(yDiff); j++) {
                    currentTile = new Tile(x, firstTileY + j, ZOOM, TILE_SIZE);

                    RendererJob theJob = new RendererJob(currentTile, mapData, rtf, dm, 1.0f, false, false);
                    // Draw tile and save as PNG.
                    TileBitmap tb = renderer.executeJob(theJob);
                    //tileCache.put(theJob, tb);
                    File tileFile = tileCache.putAndGet(theJob, tb);
                    images[i][j] = tileFile;
                    //drawLineOnImage(tileFile, currentTile);
                }
            } else {
                currentTile = new Tile(x, firstTileY, ZOOM, TILE_SIZE);
                RendererJob theJob = new RendererJob(currentTile, mapData, rtf, dm, 1.0f, false, false);
                // Draw tile and save as PNG.
                TileBitmap tb = renderer.executeJob(theJob);
                tileCache.put(theJob, tb);
                // TODO: use outputstream
                File tileFile = tileCache.putAndGet(theJob, tb);
                //PipedOutputStream test = tileCache.putAndGetStream(theJob, tb);
                // TODO: store buffered images here instead of files!!
                //InputStream valami = new PipedInputStream(test);
                //streams[i][j] = valami;

                images[i][j] = tileFile;
            }
        }
        BufferedImage combined = combineImages(images, Math.abs(xDiff) + 1, Math.abs(yDiff));
        //BufferedImage combined = combineImagesStream(streams, Math.abs(xDiff) + 1, Math.abs(yDiff));
        File stitchedImage = new File(images[0][0].getPath() + "testing");
        ImageIO.write(combined, "png", stitchedImage);

        drawLineOnImage(stitchedImage, firstTile);

        // Close map.
        mapData.close();

    }

    private static BufferedImage combineImages(File[][] imageFiles, int xSize, int ySize) throws IOException {
        BufferedImage combined = new BufferedImage(TILE_SIZE * xSize, TILE_SIZE * ySize, BufferedImage.TYPE_INT_RGB);
        Graphics g = combined.getGraphics();
        for (int i = 0; i < xSize; i++) {
            for (int j = 0; j < ySize; j++) {
                if (imageFiles[i][j] != null) {
                    BufferedImage image = ImageIO.read(imageFiles[i][j]);
                    g.drawImage(image, i * TILE_SIZE, j * TILE_SIZE, null);
                }
            }
        }
        g.dispose();
        return combined;
    }

    private static BufferedImage combineImagesStream(InputStream[][] imageStreams, int xSize, int ySize) throws IOException {
        BufferedImage combined = new BufferedImage(TILE_SIZE * xSize, TILE_SIZE * ySize, BufferedImage.TYPE_INT_RGB);
        Graphics g = combined.getGraphics();
        for (int i = 0; i < xSize; i++) {
            for (int j = 0; j < ySize; j++) {
                if (imageStreams[i][j] != null) {
                    BufferedImage image = ImageIO.read(imageStreams[i][j]);
                    g.drawImage(image, i * TILE_SIZE, j * TILE_SIZE, null);
                }
            }
        }
        g.dispose();
        return combined;
    }

    private static File drawLineOnImage(File tileFile, Tile baseTile) throws IOException {
        double tileXToPixel = MercatorProjection.tileToPixel(baseTile.tileX, TILE_SIZE);
        double tileYToPixel = MercatorProjection.tileToPixel(baseTile.tileY, TILE_SIZE);
        BufferedImage tileImage = ImageIO.read(tileFile);
        Graphics2D g = ((Graphics2D) tileImage.getGraphics());

        for (int i = 1; i < polyLinePoints.size(); i++) {
            LatLong point = polyLinePoints.get(i);
            LatLong prevPoint = polyLinePoints.get(i - 1);

            double overallMapPixelXStart = MercatorProjection.longitudeToPixelX(prevPoint.getLongitude(), ZOOM, TILE_SIZE);
            double overallMapPixelYStart = MercatorProjection.latitudeToPixelY(prevPoint.getLatitude(), ZOOM, TILE_SIZE);
            int actualXStart = Double.valueOf(Math.abs(overallMapPixelXStart- tileXToPixel)).intValue();
            int actualYStart = Double.valueOf(Math.abs(overallMapPixelYStart - tileYToPixel)).intValue();

            double overallMapPixelXEnd = MercatorProjection.longitudeToPixelX(point.getLongitude(), ZOOM, TILE_SIZE);
            double overallMapPixelYEnd = MercatorProjection.latitudeToPixelY(point.getLatitude(), ZOOM, TILE_SIZE);
            int actualXEnd = Double.valueOf(Math.abs(overallMapPixelXEnd - tileXToPixel)).intValue();
            int actualYEnd = Double.valueOf(Math.abs(overallMapPixelYEnd - tileYToPixel)).intValue();

            System.out.println(actualXStart);
            System.out.println(actualYStart);

            g.setStroke(new BasicStroke(2));
            g.setColor(Color.RED);
            g.drawLine(actualXStart, actualYStart, actualXEnd, actualYEnd);
            //g.drawOval(actualXStart, actualYStart, 10, 10);
        }

        ImageIO.write(tileImage, "png", tileFile);
        g.dispose();

        return tileFile;
    }

    private static LatLong getTopLeft() {
        BoundingBox box = new BoundingBox(polyLinePoints);
        return new LatLong(box.maxLatitude, box.minLongitude);
    }

    private static LatLong getBottomRight() {
        BoundingBox box = new BoundingBox(polyLinePoints);
        return new LatLong(box.minLatitude, box.maxLongitude);
    }

}
