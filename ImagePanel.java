import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class ImagePanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private BufferedImage image;
	private Rectangle selection;
	/**
	 * Dernière sélection faite de l'utilisateur
	 */
	public static Rectangle selected;
	/**
	 * Liste des dernières sélections faites
	 */
	public List<Rectangle> selectedAreas = new ArrayList<Rectangle>();
	private Point startPoint;

	/**
	 * Cette classe représente le panneau d'affichage de l'image dans notre fenêtre
	 * Swing
	 * 
	 * @param imageFilePath chemin d'accès vers l'image que l'on souhaite afficher
	 * @throws IOException
	 */
	public ImagePanel(String imageFilePath) throws IOException {
		try {
			image = ImageIO.read(new File(imageFilePath));
		} catch (IOException ex) {
			throw ex;
		}

		setLayout(null); // Remove the layout manager

		addMouseListener(new MouseAdapter() {
			@Override
			/**
			 * Permet de récupérer le carré de sélection dessinné par l'utilisateur
			 */
			public void mousePressed(MouseEvent e) {
				startPoint = e.getPoint();
				selection = new Rectangle(e.getX(), e.getY(), 0, 0);
				selectedAreas.add(selection);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				selection = null;
			}
		});

		addMouseMotionListener(new MouseAdapter() {
			@Override
			/**
			 * Lors d'un déplacement avec appuie de la souris on vient tracer à l'écran le rectangle de séléction
			 * @param e
			 */
			public void mouseDragged(MouseEvent e) {
				int x = Math.min(startPoint.x, e.getX());
				int y = Math.min(startPoint.y, e.getY());
				int width = Math.max(startPoint.x - e.getX(), e.getX() - startPoint.x);
				int height = Math.max(startPoint.y - e.getY(), e.getY() - startPoint.y);
				selection = new Rectangle(x, y, width, height);
				selectedAreas.add(selection);
				selected = selection;
				repaint();
			}
		});
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.drawImage(image, 0, 0, this); 
		if (selection != null) {
			g.setColor(Color.RED);
			g.drawRect(selection.x, selection.y, selection.width, selection.height);
		}
	}
/**
 * Permet d'afficher l'image sur le panel
 * @param image
 */
	public void setImage(BufferedImage image) {
		this.image = image;
		repaint();
	}
/**
 * Fonction permettant de retourner la liste des sélections faites dans l'image
 * @return une liste de sélection
 */
	public List<Rectangle> getSelectedAreas() {
		return selectedAreas;
	}
/**
 * permet d'obtenir l'image qui est actuellement affichée à l'écran
 * @return 
 */
	public BufferedImage getImage() {
		return image;
	}
}
