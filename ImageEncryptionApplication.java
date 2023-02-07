
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ImageEncryptionApplication extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextField imagePathField;
	private JPasswordField psswdField;
	private JPasswordField keyField = new JPasswordField(20);
	private JButton selectImageButton;
	private JButton encryptButton;
	private JButton decryptButton;
	private JButton saveButton;
	private ImagePanel imagePanel;
	private BufferedImage image;
	private String selectedItem = "AES";
	private File selectedFile;
	private SecretKey rc4Key;
	private SecretKey aesKey;
	private SecretKeySpec secretKeySpec;
	private byte[] iv;

	private List<byte[]> saltList = new ArrayList<>();
	private List<String> psswdList = new ArrayList<>();

	private byte[] encryptedImageBytes;

	/**
	 * Constructeur de l'application, comprends les méthodes de chiffrage et
	 * déchiffrage
	 * 
	 * @throws Exception
	 */
	public ImageEncryptionApplication() throws Exception {
		// Set up the main window
		setTitle("Image Encryption Tool");
		setSize(600, 400);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		// Create a panel to hold the input fields and buttons
		JPanel inputPanel = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();

		// Add a label and text field for the image path
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.WEST;
		inputPanel.add(new JLabel("Image path: "), constraints);

		imagePathField = new JTextField(20);
		constraints.gridx = 1;
		inputPanel.add(imagePathField, constraints);

		// Add a button to select an image file
		selectImageButton = new JButton("Select image");
		selectImageButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Show a file chooser to select an image file
				JFileChooser fileChooser = new JFileChooser();
				int result = fileChooser.showOpenDialog(ImageEncryptionApplication.this);
				if (result == JFileChooser.APPROVE_OPTION) {

					// Set the selected file as the image path
					String selectedImageFilePath = fileChooser.getSelectedFile().getAbsolutePath();
					imagePathField.setText(selectedImageFilePath);
					imagePanel = new ImagePanel(selectedImageFilePath);
					try {
						selectedFile = fileChooser.getSelectedFile();
						image = ImageIO.read(selectedFile);
						imagePanel.setImage(image);
					} catch (IOException e1) {
						JOptionPane.showInternalMessageDialog(inputPanel, e1);
					}
					// Add the new ImagePanel to the interface
					add(imagePanel, BorderLayout.CENTER);
					// Revalidate and repaint the interface to show the new ImagePanel

					revalidate();
					repaint();
				}
			}
		});

		constraints.gridx = 2;
		inputPanel.add(selectImageButton, constraints);

		// Add a label and text field for the encryption key
		constraints.gridx = 0;
		constraints.gridy = 1;
		inputPanel.add(new JLabel("Encryption key: "), constraints);

		psswdField = new JPasswordField(20);
		constraints.gridx = 1;
		inputPanel.add(psswdField, constraints);

		// Add the input panel to the main window
		add(inputPanel, BorderLayout.NORTH);

		// Create a panel to hold the buttons
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

		// Create a combo box to choose the encryption algorithm
		String[] options = { "AES", "RC4", "AESWithPsswd","Blowfish" };
		JComboBox<String> comboBox = new JComboBox<>(options);
		buttonPanel.add(comboBox);

		// Add an encrypt button
		comboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectedItem = (String) comboBox.getSelectedItem();
			}
		});
		encryptButton = new JButton("Encrypt");
		encryptButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if(selectedItem == "Blowfish") {
						encryptBlowfish();
					}
					else {
						encryptSelectedAreas(selectedItem);
					}
					
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});

		buttonPanel.add(encryptButton, BorderLayout.CENTER);

		// Add an decrypt button
		decryptButton = new JButton("Decrypt");
		decryptButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if(selectedItem == "Blowfish") {
						decryptBlowfish();
					}
					else {
					decrypt(selectedItem);
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});

		buttonPanel.add(decryptButton, BorderLayout.CENTER);

		saveButton = new JButton("Save");
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Save();
			}
		});

		buttonPanel.add(saveButton, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);

	}

	/**
	 * Fonction permettant l'enregistrement de l'image encryptée en png reprend le
	 * nom de base de l'image et y ajoute "_encrypted" enregistre à côté dans un
	 * fichier xml la zone d'encryption enfin, dans fichier binaire se trouve
	 * également la clef de chiffrement pour AES
	 */
	public void Save() {
		// Créer un nouveau fichier avec le nom de l'image d'origine et l'extension .png
		File file = new File(
				selectedFile.getName().substring(0, selectedFile.getName().lastIndexOf(".")) + "_encrypted" + ".png");

		// Enregistrer l'image chiffré dans le nouveau fichier
		try {
			ImageIO.write(imagePanel.getImage(), "png", file);
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		// Afficher une boîte de dialogue pour choisir où enregistrer le fichier chiffrée
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setSelectedFile(file);
		if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
			// Rï¿½cupï¿½rer le fichier sï¿½lectionnï¿½
			File selectedFile = fileChooser.getSelectedFile();

			// Enregistrer le fichier chiffrï¿½
			try {
				ImageIO.write(imagePanel.getImage(), "png", selectedFile);

				// Récupère le chemin d'enregistrement de l'image
				String filePath = selectedFile.getAbsolutePath();
				DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
				Document doc = docBuilder.newDocument();

				// Créer la racine "selection"
				Element rootElement = doc.createElement("selection");
				doc.appendChild(rootElement);

				// Ajouter les attributs "x", "y", "width" et "height" dans une propriété
				// "selection"
				Attr x = doc.createAttribute("x");
				x.setValue(String.valueOf(ImagePanel.selected.x));
				rootElement.setAttributeNode(x);

				Attr y = doc.createAttribute("y");
				y.setValue(String.valueOf(ImagePanel.selected.y));
				rootElement.setAttributeNode(y);

				Attr width = doc.createAttribute("width");
				width.setValue(String.valueOf(ImagePanel.selected.width));
				rootElement.setAttributeNode(width);

				Attr height = doc.createAttribute("height");
				height.setValue(String.valueOf(ImagePanel.selected.height));
				rootElement.setAttributeNode(height);

				// Ecrire le document dans un fichier
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer;
				try {
					transformer = transformerFactory.newTransformer();
					DOMSource source = new DOMSource(doc);
					String fileName = filePath.substring(0, filePath.lastIndexOf(".")) + "_data.xml";
					StreamResult result = new StreamResult(new File(fileName));
					transformer.transform(source, result);

					if(selectedItem == "AES" || selectedItem == "AESWithPsswd") {
						File keyFile = new File(selectedFile.getParentFile(),"key.bin");
						FileOutputStream fos = new FileOutputStream(keyFile);
						fos.write(aesKey.getEncoded());
						fos.close();
					}
					if(selectedItem == "RC4") {
						File keyFileRC4 = new File(selectedFile.getParentFile(),"RC4key.bin");
						
						FileOutputStream fosRC4 = new FileOutputStream(keyFileRC4);
						fosRC4.write(rc4Key.getEncoded());
						fosRC4.close();					
					}
					if(selectedItem == "Blowfish") {
						File keyFileBlowfish = new File(selectedFile.getParentFile(),"Blowfish.bin");
						
						FileOutputStream fosBlowfish = new FileOutputStream(keyFileBlowfish);
						fosBlowfish.write(secretKeySpec.getEncoded());
						fosBlowfish.close();					
					}
				} catch (TransformerConfigurationException e1) {
					e1.printStackTrace();
				} catch (TransformerException e1) {
					e1.printStackTrace();
				}

			} catch (IOException ex) {
				ex.printStackTrace();
			} catch (ParserConfigurationException e2) {
				e2.printStackTrace();
			}

			// Afficher un message de confirmation
			JOptionPane.showMessageDialog(null, "L'image a été enregistrée avec succès !", "Succès",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}
	
	
	private void encryptBlowfish() throws Exception {
		// Lecture du mot de passe
		char[] enteredPassword = keyField.getPassword();
		byte[] enteredPasswordBytes = new String(enteredPassword).getBytes();

		// Creation de la clé de cryptage
		secretKeySpec = new SecretKeySpec(enteredPasswordBytes, "Blowfish");
		Cipher cipher = Cipher.getInstance("Blowfish");
		cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

		// Converstion de l'image en tableau de bytes
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, "png", baos);
		byte[] imagebytes = baos.toByteArray();

		// Cryptage de l'image
		byte[] encryptedImageBytes = cipher.doFinal(imagebytes);

		// Conversion du tableau de l'image crypté en image
		InputStream is = new ByteArrayInputStream(encryptedImageBytes);
		BufferedImage bi = ImageIO.read(is);
		ImageIO.write(bi, "png", new File("C:\\TEST\\encryptedImage.png"));
	}

	private void decryptBlowfish() throws Exception{
		//Lecture du mot de passe
		char[] enteredPassword = keyField.getPassword();
		byte[] enteredPasswordBytes = new String(enteredPassword).getBytes();
		
		//Creation de la clé de cryptage
		SecretKeySpec secretKeySpec = new SecretKeySpec(enteredPasswordBytes, "Blowfish");
		Cipher cipher = Cipher.getInstance("Blowfish");
		cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
		
		//Converstion de l'image en tableau de bytes
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, "png", baos);
		byte[] imagebytes = baos.toByteArray();
		
		//Decryptage de l'image
		byte[] decodedImage = cipher.doFinal(imagebytes);
		
		// Conversion du tableau de l'image crypté en image
		InputStream is = new ByteArrayInputStream(decodedImage);
		BufferedImage bi = ImageIO.read(is);
		ImageIO.write(bi, "png", new File("C:\\TEST\\decryptedImage.png"));
		
		
	}
	
	
	
	
/**
 * Fonction réalisant le décryptage de l'image se trouvant être codé
 * Dans le cas d'AES il y a deux possibilités : 
 * 	- l'image contient la mention encrypted dans son nom, dans ce cas on va essayer de lire le fichier xml contenant la position du rectangle de l'encryption
 * 	- l'image ne contient pas la mention encrypted, dans ce cas on vient lire la dernière sélection faite et la clef en cache
 * @param select Algorithme sélectionné dans le composant comboBox
 * @throws Exception
 */
	public void decrypt(String select) throws Exception {

		
		// List<Rectangle> selectedAreas = imagePanel.getSelectedAreas();
		
		// Récupération des données de l'image sous forme de tableau de bytes
		byte[] imageData = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		switch (select) {
		case ("RC4"):
			char[] enteredPassword = keyField.getPassword();
			byte[] enteredPasswordBytes = new String(enteredPassword).getBytes();
			byte[] keyBytesRC4 = rc4Key.getEncoded();
			if(enteredPasswordBytes == keyBytesRC4) {
				String path = selectedFile.getPath();
				String fileName = selectedFile.getName();
	
				int indexOfDot = path.lastIndexOf(".");
				String fileNameWithoutExtension = path.substring(0, indexOfDot);
				if (fileName.contains("encrypted")) {
					Cipher cipher = Cipher.getInstance("RC4");
		
					cipher.init(Cipher.DECRYPT_MODE, rc4Key);
					byte[] decryptedImageBytes = cipher.doFinal(encryptedImageBytes);
		
					encryptedImageBytes = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
					Document doc = builder.parse(new File(fileNameWithoutExtension + "_dataRC4.xml"));
	
					Element root = doc.getDocumentElement();
					int x1 = Integer.parseInt(root.getAttribute("x"));
					int y1 = Integer.parseInt(root.getAttribute("y"));
					int w1 = Integer.parseInt(root.getAttribute("width"));
					int h1 = Integer.parseInt(root.getAttribute("height"));
		
					
					FileInputStream keyFile = new FileInputStream(selectedFile.getParentFile() + "/RC4key.bin");
					byte[] keyBytes = new byte[keyFile.available()];
					keyFile.read(keyBytes);
					keyFile.close();
	
					SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "RC4");
	
	
					iv = new byte[16];
					iv[0] = 6;
					iv[1] = 9;
					iv[2] = 1;
					iv[3] = 2;
					iv[4] = 5;
					iv[5] = 37;
					iv[6] = 78;
					iv[7] = 34;
					iv[8] = 89;
					iv[9] = 78;
					iv[10] = 90;
					iv[11] = 23;
					iv[12] = 111;
					iv[13] = 93;
					iv[14] = 35;
					iv[15] = 5;
					IvParameterSpec ivSpec = new IvParameterSpec(iv);
					cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
					
					
					for (int i = 0; i < h1; i++) {
						for (int j = 0; j < w1; j++) {
							int pixelIndex = (x1 + j + (y1 + i) * image.getWidth()) * 3;
							for (int k = 0; k < 3; k++) {
								// Appliquer le chiffrement Ã  chaque composante R, G, B du pixel
								imageData[pixelIndex + k] = (byte) (imageData[pixelIndex + k]
										^ decryptedImageBytes[k % decryptedImageBytes.length]
										^ decryptedImageBytes[k % decryptedImageBytes.length]);
							}
						}
					}
				}
			}else {
	
				JOptionPane.showMessageDialog(this, "La clef de déchiffrage est incorrect", "Erreur",
					JOptionPane.ERROR_MESSAGE);
			}
			
			break;
		case ("AES"):

			try {
				String path = selectedFile.getPath();
				String fileName = selectedFile.getName();

				int indexOfDot = path.lastIndexOf(".");
				String fileNameWithoutExtension = path.substring(0, indexOfDot);

				if (fileName.contains("encrypted")) {

					encryptedImageBytes = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
					Document doc = builder.parse(new File(fileNameWithoutExtension + "_data.xml"));

					Element root = doc.getDocumentElement();
					int x1 = Integer.parseInt(root.getAttribute("x"));
					int y1 = Integer.parseInt(root.getAttribute("y"));
					int w1 = Integer.parseInt(root.getAttribute("width"));
					int h1 = Integer.parseInt(root.getAttribute("height"));

					FileInputStream keyFile = new FileInputStream(selectedFile.getParentFile() + "/key.bin");
					byte[] keyBytes = new byte[keyFile.available()];
					keyFile.read(keyBytes);
					keyFile.close();

					SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

					Cipher aesCipher = Cipher.getInstance("AES/CFB/NoPadding");
					iv = new byte[16];
					iv[0] = 6;
					iv[1] = 9;
					iv[2] = 1;
					iv[3] = 2;
					iv[4] = 5;
					iv[5] = 37;
					iv[6] = 78;
					iv[7] = 34;
					iv[8] = 89;
					iv[9] = 78;
					iv[10] = 90;
					iv[11] = 23;
					iv[12] = 111;
					iv[13] = 93;
					iv[14] = 35;
					iv[15] = 5;
					IvParameterSpec ivSpec = new IvParameterSpec(iv);
					aesCipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

					for (int i = 0; i < h1; i++) {
						for (int j = 0; j < w1; j++) {
							int pixelIndex = (x1 + j + (y1 + i) * image.getWidth()) * 3;
							for (int k = 0; k < 3; k++) {
								byte[] decryptedPixel = aesCipher.doFinal(new byte[] { imageData[pixelIndex + k] });
								imageData[pixelIndex + k] = decryptedPixel[0];
							}
						}
					}
					// Mise Ã  jour de l'image affichÃ©e avec les donnÃ©es dÃ©chiffrÃ©es
					image.setData(Raster.createRaster(image.getSampleModel(),
							new DataBufferByte(imageData, imageData.length), new Point()));
					repaint();
					JOptionPane.showMessageDialog(null, "L'image a été déchiffrée avec succès !", "Succès",
							JOptionPane.INFORMATION_MESSAGE);
				}				
				else if (aesKey != null && ImagePanel.selected != null) {
					Cipher aesCipher = Cipher.getInstance("AES/CFB/NoPadding");
					iv = new byte[16];
					iv[0] = 6;
					iv[1] = 9;
					iv[2] = 1;
					iv[3] = 2;
					iv[4] = 5;
					iv[5] = 37;
					iv[6] = 78;
					iv[7] = 34;
					iv[8] = 89;
					iv[9] = 78;
					iv[10] = 90;
					iv[11] = 23;
					iv[12] = 111;
					iv[13] = 93;
					iv[14] = 35;
					iv[15] = 5;
					IvParameterSpec ivSpec = new IvParameterSpec(iv);
					aesCipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);

					for (int i = 0; i < ImagePanel.selected.height; i++) {
						for (int j = 0; j < ImagePanel.selected.width; j++) {
							int pixelIndex = (ImagePanel.selected.x + j
									+ (ImagePanel.selected.y + i) * image.getWidth()) * 3;
							for (int k = 0; k < 3; k++) {
								byte[] decryptedPixel = aesCipher.doFinal(new byte[] { imageData[pixelIndex + k] });
								imageData[pixelIndex + k] = decryptedPixel[0];
							}
						}
					}
					// Mise Ã  jour de l'image affichÃ©e avec les donnÃ©es dÃ©chiffrÃ©es
					image.setData(Raster.createRaster(image.getSampleModel(),
							new DataBufferByte(imageData, imageData.length), new Point()));
					repaint();
					JOptionPane.showMessageDialog(null, "L'image a été déchiffrée avec succès !", "Succès",
							JOptionPane.INFORMATION_MESSAGE);
				}
				else if (aesKey == null || ImagePanel.selected == null) {
					JOptionPane.showMessageDialog(null, "Erreur lors du déchiffrement de l'image, impossible d'obtenir la clef ou la zone d'encryption ", "Erreur", JOptionPane.ERROR_MESSAGE);
				}
				
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(null, "Erreur lors du dÃ©chiffrement de l'image : " + ex.getMessage(),
						"Erreur", JOptionPane.ERROR_MESSAGE);
			}
			break;

		case ("AESWithPsswd"):

			try {

				String path = selectedFile.getPath();
				String fileName = selectedFile.getName();

				int indexOfDot = path.lastIndexOf(".");
				String fileNameWithoutExtension = path.substring(0, indexOfDot);

				String psswd = psswdField.getPassword().toString();
				String psswdSaved;

				Scanner s = new Scanner(new File("pair.txt"));
				ArrayList<String> list = new ArrayList<String>();
				while (s.hasNextLine()) {
					list.add(s.nextLine());
				}
				s.close();
				for (int u = 0; u < list.size(); u++) {
					psswdSaved = list.get(u);
					psswd = list.get(u);
					if (psswd == psswdSaved) {

						if (fileName.contains("encrypted")) {

							encryptedImageBytes = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
							DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
							DocumentBuilder builder = factory.newDocumentBuilder();
							Document doc = builder.parse(new File(fileNameWithoutExtension + "_data.xml"));

							Element root = doc.getDocumentElement();
							int x1 = Integer.parseInt(root.getAttribute("x"));
							int y1 = Integer.parseInt(root.getAttribute("y"));
							int w1 = Integer.parseInt(root.getAttribute("width"));
							int h1 = Integer.parseInt(root.getAttribute("height"));

							// Rectangle enctyptedSelection = new Rectangle(x1, y1, w1, h1);

							FileInputStream keyFile = new FileInputStream(selectedFile.getParentFile() + "/key.bin");
							byte[] keyBytes = new byte[keyFile.available()];
							keyFile.read(keyBytes);
							keyFile.close();

							SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

							Cipher aesCipher = Cipher.getInstance("AES/CFB/NoPadding");
							iv = new byte[16];
							iv[0] = 6;
							iv[1] = 9;
							iv[2] = 1;
							iv[3] = 2;
							iv[4] = 5;
							iv[5] = 37;
							iv[6] = 78;
							iv[7] = 34;
							iv[8] = 89;
							iv[9] = 78;
							iv[10] = 90;
							iv[11] = 23;
							iv[12] = 111;
							iv[13] = 93;
							iv[14] = 35;
							iv[15] = 5;
							IvParameterSpec ivSpec = new IvParameterSpec(iv);
							aesCipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

							for (int i = 0; i < h1; i++) {
								for (int j = 0; j < w1; j++) {
									int pixelIndex = (x1 + j + (y1 + i) * image.getWidth()) * 3;
									for (int k = 0; k < 3; k++) {
										byte[] decryptedPixel = aesCipher
												.doFinal(new byte[] { imageData[pixelIndex + k] });
										imageData[pixelIndex + k] = decryptedPixel[0];
									}
								}
							}
						}
					}
				}
				
				// Mise Ã  jour de l'image affichÃ©e avec les donnÃ©es dÃ©chiffrÃ©es
				image.setData(Raster.createRaster(image.getSampleModel(),
						new DataBufferByte(imageData, imageData.length), new Point()));
				repaint();
				JOptionPane.showMessageDialog(null, "L'image a été déchiffrée avec succès !", "Succès",
						JOptionPane.INFORMATION_MESSAGE);
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(null, "Erreur lors du dÃ©chiffrement de l'image : " + ex.getMessage(),
						"Erreur", JOptionPane.ERROR_MESSAGE);
			}

		}

	}
	
/**
 * Fonction permettant d'encrypter les pixels d'une image dans la zone que l'utilisateur a choisi
 * Dans le cas d'AES, on vient générer une clef à la volée et on applique à chaque composante RGB d'un pixel un chiffrement AES, sans padding
 * @param select Algorithme sélectionné pour l'encryptage
 * @throws Exception
 */
	public void encryptSelectedAreas(String select) throws Exception {
		
		// Récupération des zones sélectionnées par l'utilisateur
		// List<Rectangle> selectedAreas = imagePanel.getSelectedAreas();
		//Récupération des données de l'image sous forme de tableau de bytes
		byte[] imageData = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

		switch (select) {
		case ("RC4"):
			KeyGenerator keyGenerator = KeyGenerator.getInstance("RC4");
			keyGenerator.init(128); // key size
			rc4Key = keyGenerator.generateKey();
			if(rc4Key == null) {
			    // Creating a key
			    byte[] keyBytes = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F};
			    rc4Key = new SecretKeySpec(keyBytes, "RC4");
			}
			Cipher cipher = Cipher.getInstance("RC4");
			cipher.init(Cipher.ENCRYPT_MODE, rc4Key);
			encryptedImageBytes = cipher.doFinal(imageData);
			int x = (int) ImagePanel.selected.getX();
			int y = (int) ImagePanel.selected.getY();
			int w = (int) ImagePanel.selected.getWidth();
			int h = (int) ImagePanel.selected.getHeight();

			for (int i = 0; i < h; i++) {
				for (int j = 0; j < w; j++) {
					int pixelIndex = (x + j + (y + i) * image.getWidth()) * 3;
					for (int k = 0; k < 3; k++) {
						// Appliquer le chiffrement à  chaque composante R, G, B du pixel
						imageData[pixelIndex + k] = (byte) (imageData[pixelIndex + k]
								^ encryptedImageBytes[k % encryptedImageBytes.length]);
					}
				}
			}
			break;
		case ("AES"):
			KeyGenerator keyGeneratorAes = KeyGenerator.getInstance("AES");
			keyGeneratorAes.init(128);
			aesKey = keyGeneratorAes.generateKey();
			iv = new byte[16];
			iv[0] = 6;
			iv[1] = 9;
			iv[2] = 1;
			iv[3] = 2;
			iv[4] = 5;
			iv[5] = 37;
			iv[6] = 78;
			iv[7] = 34;
			iv[8] = 89;
			iv[9] = 78;
			iv[10] = 90;
			iv[11] = 23;
			iv[12] = 111;
			iv[13] = 93;
			iv[14] = 35;
			iv[15] = 5;

			IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
			Cipher aesCipher = Cipher.getInstance("AES/CFB/NoPadding");
			aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, ivParameterSpec);
			
			int x1 = (int) ImagePanel.selected.getX();
			int y1 = (int) ImagePanel.selected.getY();
			int w1 = (int) ImagePanel.selected.getWidth();
			int h1 = (int) ImagePanel.selected.getHeight();
			for (int i = 0; i < h1; i++) {
				for (int j = 0; j < w1; j++) {
					int pixelIndex = (x1 + j + (y1 + i) * image.getWidth()) * 3;
					for (int k = 0; k < 3; k++) {
						// Appliquer le chiffrement à  chaque composante R, G, B du pixel
						byte[] encryptedPixel = aesCipher.doFinal(new byte[] { imageData[pixelIndex + k] });
						imageData[pixelIndex + k] = encryptedPixel[0];
					}
				}
			}
			break;

		case ("AESWithPsswd"):
			iv = new byte[16];
			iv[0] = 6;
			iv[1] = 9;
			iv[2] = 1;
			iv[3] = 2;
			iv[4] = 5;
			iv[5] = 37;
			iv[6] = 78;
			iv[7] = 34;
			iv[8] = 89;
			iv[9] = 78;
			iv[10] = 90;
			iv[11] = 23;
			iv[12] = 111;
			iv[13] = 93;
			iv[14] = 35;
			iv[15] = 5;

			IvParameterSpec ivParameterSpec2 = new IvParameterSpec(iv);
			Random r = new SecureRandom();
			byte[] salt = new byte[8];
			r.nextBytes(salt);

			char[] passwdChar = psswdField.getPassword();
			psswdList.add(passwdChar.toString());

			Path output = Paths.get("psswd.txt");

			try {
				Files.write(output, psswdList);
			} catch (Exception e) {
				e.printStackTrace();
			}

			PBEKeySpec pbeKeySpec = new PBEKeySpec(passwdChar, salt, 1024, 256);
			SecretKey tmp = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(pbeKeySpec);
			SecretKey aesKey = new SecretKeySpec(tmp.getEncoded(), "AES");

			try {
				Files.write(output, salt);

				Files.write(output, psswdList);
			} catch (Exception e) {
				e.printStackTrace();
			}

			Cipher aesCipher2 = Cipher.getInstance("AES/CFB/NoPadding");
			aesCipher2.init(Cipher.ENCRYPT_MODE, aesKey, ivParameterSpec2);


			int x2 = (int) ImagePanel.selected.getX();
			int y2 = (int) ImagePanel.selected.getY();
			int w2 = (int) ImagePanel.selected.getWidth();
			int h2 = (int) ImagePanel.selected.getHeight();
			for (int i = 0; i < h2; i++) {
				for (int j = 0; j < w2; j++) {
					int pixelIndex = (x2 + j + (y2 + i) * image.getWidth()) * 3;
					for (int k = 0; k < 3; k++) {
						// Appliquer le chiffrement à  chaque composante R, G, B du pixel
						byte[] encryptedPixel = aesCipher2.doFinal(new byte[] { imageData[pixelIndex + k] });
						imageData[pixelIndex + k] = encryptedPixel[0];
					}
				}
			}
			
			break;

		}
		// Mise à  jour de l'affichage de l'image
		imagePanel.repaint();
	}

	/**
	 * Lance l'application et la rend visible
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		ImageEncryptionApplication app = new ImageEncryptionApplication();

		app.setVisible(true);
	}

}
