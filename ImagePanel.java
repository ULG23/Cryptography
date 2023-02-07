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
	 * Derni�re s�lection faite de l'utilisateur
	 */
	public static Rectangle selected;
	/**
	 * Liste des derni�res s�lections faites
	 */
	public List<Rectangle> selectedAreas = new ArrayList<Rectangle>();
	private Point startPoint;

	/**
	 * Cette classe repr�sente le panneau d'affichage de l'image dans notre fen�tre
	 * Swing
	 * 
	 * @param imageFilePath chemin d'acc�s vers l'image que l'on souhaite afficher
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
			 * Permet de r�cup�rer le carr� de s�lection dessinn� par l'utilisateur
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
			 * Lors d'un d�placement avec appuie de la souris on vient tracer � l'�cran le rectangle de s�l�ction
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
 * Fonction permettant de retourner la liste des s�lections faites dans l'image
 * @return une liste de s�lection
 */
	public List<Rectangle> getSelectedAreas() {
		return selectedAreas;
	}
/**
 * permet d'obtenir l'image qui est actuellement affich�e � l'�cran
 * @return 
 */
	public BufferedImage getImage() {
		return image;
	}
}
