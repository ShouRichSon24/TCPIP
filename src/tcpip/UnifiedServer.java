import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Unified TCP Server - Gabungan Tugas No. 3, 4, 5
 * Fitur:
 * - Tab 1: Menerima & menyimpan data Mahasiswa (Object Serialization)
 * - Tab 2: Mengirim & menerima gambar
 * - Tab 3: Mengirim & menerima pesan suara
 */
public class UnifiedServer extends JFrame {

    private static final int SERVICE_PORT = 5005;

    // Folder penyimpanan
    private static final String DIR_DATA = "data";
    private static final String DIR_IMAGES = "images";
    private static final String DIR_VOICE = "voice";

    // Common
    private JLabel statusLabel;
    private JTextArea logArea;
    private JButton startBtn;
    private Socket clientSocket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private boolean isRunning = false;

    // Tab Mahasiswa
    private DefaultTableModel tableModel;
    private JTable dataTable;

    // Tab Image
    private JLabel imageLabel;
    private JPanel imagePanel;
    private JButton sendImageBtn;

    // Tab Voice
    private JButton recordBtn, stopRecordBtn, sendVoiceBtn, playVoiceBtn;
    private JLabel voiceStatusLabel;
    private TargetDataLine microphone;
    private ByteArrayOutputStream audioBuffer;
    private boolean isRecording = false;
    private byte[] lastRecordedAudio;
    private byte[] lastReceivedAudio;
    private AudioFormat audioFormat;

    public UnifiedServer() {
        setTitle("Unified TCP Server - Port " + SERVICE_PORT);
        setSize(750, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        audioFormat = new AudioFormat(16000, 16, 1, true, false);
        initFolders();
        initUI();
    }

    /** Buat folder penyimpanan jika belum ada */
    private void initFolders() {
        new File(DIR_DATA).mkdirs();
        new File(DIR_IMAGES).mkdirs();
        new File(DIR_VOICE).mkdirs();
    }

    private void initUI() {
        setLayout(new BorderLayout(5, 5));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(8, 8, 8, 8));

        // === TOP ===
        JPanel topPanel = new JPanel(new BorderLayout(8, 0));
        statusLabel = new JLabel("  Status: Stopped");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        statusLabel.setForeground(Color.RED);
        startBtn = new JButton("Start Server");
        startBtn.addActionListener(e -> startServer());
        topPanel.add(statusLabel, BorderLayout.CENTER);
        topPanel.add(startBtn, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // === CENTER: Tabs ===
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 12));
        tabs.addTab("Data Mahasiswa", createMahasiswaTab());
        tabs.addTab("Gambar", createImageTab());
        tabs.addTab("Pesan Suara", createVoiceTab());
        add(tabs, BorderLayout.CENTER);

        // === BOTTOM: Log ===
        logArea = new JTextArea(7, 60);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log Aktivitas"));
        add(logScroll, BorderLayout.SOUTH);
    }

    // ==================== TAB MAHASISWA ====================
    private JPanel createMahasiswaTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        String[] columns = { "No", "NIM", "Nama", "Asal", "Kelas Praktikum", "Waktu" };
        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        dataTable = new JTable(tableModel);
        dataTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        dataTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        dataTable.setRowHeight(24);
        dataTable.getColumnModel().getColumn(0).setPreferredWidth(30);

        JScrollPane tableScroll = new JScrollPane(dataTable);
        panel.add(tableScroll, BorderLayout.CENTER);

        JLabel info = new JLabel("  Data mahasiswa akan muncul otomatis saat client mengirim data.");
        info.setFont(new Font("SansSerif", Font.ITALIC, 11));
        info.setForeground(Color.GRAY);
        panel.add(info, BorderLayout.SOUTH);

        return panel;
    }

    // ==================== TAB IMAGE ====================
    private JPanel createImageTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        imagePanel = new JPanel(new BorderLayout());
        imagePanel.setBorder(BorderFactory.createTitledBorder("Gambar Diterima"));
        imagePanel.setPreferredSize(new Dimension(600, 300));
        imageLabel = new JLabel("Belum ada gambar", SwingConstants.CENTER);
        imageLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
        imageLabel.setForeground(Color.GRAY);
        imagePanel.add(imageLabel, BorderLayout.CENTER);
        panel.add(imagePanel, BorderLayout.CENTER);

        sendImageBtn = new JButton("Kirim Gambar ke Client");
        sendImageBtn.setEnabled(false);
        sendImageBtn.addActionListener(e -> sendImage());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.add(sendImageBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ==================== TAB VOICE ====================
    private JPanel createVoiceTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));

        JPanel recPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 15));
        recordBtn = new JButton("Rekam Suara");
        stopRecordBtn = new JButton("Stop Rekam");
        recordBtn.setEnabled(false);
        stopRecordBtn.setEnabled(false);
        recordBtn.setBackground(new Color(220, 50, 50));
        recordBtn.setForeground(Color.WHITE);
        recordBtn.setOpaque(true);
        recordBtn.addActionListener(e -> startRecording());
        stopRecordBtn.addActionListener(e -> stopRecording());
        recPanel.add(recordBtn);
        recPanel.add(stopRecordBtn);
        controlPanel.add(recPanel);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        sendVoiceBtn = new JButton("Kirim Rekaman ke Client");
        playVoiceBtn = new JButton("Play Terakhir Diterima");
        sendVoiceBtn.setEnabled(false);
        playVoiceBtn.setEnabled(false);
        sendVoiceBtn.addActionListener(e -> sendVoice());
        playVoiceBtn.addActionListener(e -> playReceivedAudio());
        actionPanel.add(sendVoiceBtn);
        actionPanel.add(playVoiceBtn);
        controlPanel.add(actionPanel);

        voiceStatusLabel = new JLabel("Idle", SwingConstants.CENTER);
        voiceStatusLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        voiceStatusLabel.setForeground(Color.GRAY);
        voiceStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.add(Box.createVerticalStrut(10));
        controlPanel.add(voiceStatusLabel);

        panel.add(controlPanel, BorderLayout.CENTER);
        return panel;
    }

    // ==================== LOG ====================
    private void log(String msg) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + time + "] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // ==================== SERVER ====================
    private void startServer() {
        if (isRunning)
            return;
        startBtn.setEnabled(false);

        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(SERVICE_PORT);
                isRunning = true;
                updateStatus("Waiting for client...", new Color(200, 150, 0));
                log("Server started on port " + SERVICE_PORT);

                while (isRunning) {
                    clientSocket = serverSocket.accept();
                    dis = new DataInputStream(clientSocket.getInputStream());
                    dos = new DataOutputStream(clientSocket.getOutputStream());

                    String info = clientSocket.getInetAddress().getHostAddress()
                            + ":" + clientSocket.getPort();
                    log("Client connected: " + info);
                    updateStatus("Client Connected (" + info + ")", new Color(0, 150, 0));

                    SwingUtilities.invokeLater(() -> {
                        sendImageBtn.setEnabled(true);
                        recordBtn.setEnabled(true);
                    });

                    // Loop menerima pesan dari client
                    handleClient();

                    updateStatus("Waiting for client...", new Color(200, 150, 0));
                    SwingUtilities.invokeLater(() -> {
                        sendImageBtn.setEnabled(false);
                        recordBtn.setEnabled(false);
                        sendVoiceBtn.setEnabled(false);
                    });
                    log("Client disconnected. Waiting for next...");
                }
            } catch (IOException ioe) {
                if (isRunning)
                    log("Error: " + ioe.getMessage());
            }
        }).start();
    }

    private void updateStatus(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("  Status: " + text);
            statusLabel.setForeground(color);
        });
    }

    /**
     * Protokol sederhana:
     * 1. Client kirim tipe pesan (UTF String): "MAHASISWA", "IMAGE", "VOICE"
     * 2. Client kirim panjang data (int)
     * 3. Client kirim data (bytes)
     * Untuk IMAGE: tambahan nama file (UTF) sebelum data
     */
    private void handleClient() {
        try {
            log("Waiting for data from client...");
            while (true) {
                String type = dis.readUTF(); // Baca tipe pesan
                log("[DEBUG] Received type: " + type);
                
                switch (type) {
                    case "MAHASISWA":
                        receiveMahasiswa();
                        break;
                    case "IMAGE":
                        receiveImage();
                        break;
                    case "VOICE":
                        receiveVoice();
                        break;
                    case "MESSAGE":
                        receiveMessage();
                        break;
                    default:
                        log("Unknown message type: " + type);
                }
            }
        } catch (EOFException eof) {
            // Client disconnected normally
        } catch (IOException ioe) {
            log("Connection lost: " + ioe.getMessage());
        }
    }

    // ==================== RECEIVE MAHASISWA ====================
    private static final String CSV_FILE = DIR_DATA + File.separator + "data_mahasiswa.csv";

    private void receiveMahasiswa() throws IOException {
        int length = dis.readInt();
        byte[] data = readBytes(length);

        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
            Mahasiswa mhs = (Mahasiswa) ois.readObject();
            ois.close();

            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            int no = tableModel.getRowCount() + 1;

            SwingUtilities.invokeLater(() -> {
                tableModel.addRow(new Object[] {
                        no, mhs.getNim(), mhs.getNama(), mhs.getAsal(),
                        mhs.getKelasPraktikum(), time
                });
            });

            log("[MAHASISWA] " + mhs.toString());

            // Simpan ke CSV (bisa dibuka langsung di Excel)
            File csvFile = new File(CSV_FILE);
            boolean isNew = !csvFile.exists();
            PrintWriter writer = new PrintWriter(new FileWriter(CSV_FILE, true), true);

            // Tulis header jika file baru
            if (isNew) {
                writer.println("No,NIM,Nama,Asal,Kelas Praktikum,Waktu,Client");
            }

            String clientInfo = clientSocket.getInetAddress().getHostAddress()
                + ":" + clientSocket.getPort();
            writer.println(no + ","
                + escapeCsv(mhs.getNim()) + ","
                + escapeCsv(mhs.getNama()) + ","
                + escapeCsv(mhs.getAsal()) + ","
                + escapeCsv(mhs.getKelasPraktikum()) + ","
                + time + ","
                + clientInfo);
            writer.close();

            log("[MAHASISWA] Saved to " + CSV_FILE);

        } catch (ClassNotFoundException cnfe) {
            log("Error: " + cnfe.getMessage());
        }
    }

    /** Escape value untuk CSV (handle koma dan kutip) */
    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ==================== RECEIVE IMAGE ====================
    private void receiveImage() throws IOException {
        String fileName = dis.readUTF();
        int length = dis.readInt();
        byte[] imageData = readBytes(length);

        log("[IMAGE] Received: " + fileName + " (" + length + " bytes)");

        // Save to images folder
        String savePath = DIR_IMAGES + File.separator + fileName;
        FileOutputStream fos = new FileOutputStream(savePath);
        fos.write(imageData);
        fos.close();
        log("[IMAGE] Saved to: " + savePath);

        // Display
        ImageIcon icon = new ImageIcon(imageData);
        Image img = scaleImage(icon.getImage(), imagePanel.getWidth() - 20, imagePanel.getHeight() - 40);
        final ImageIcon displayIcon = new ImageIcon(img);
        SwingUtilities.invokeLater(() -> {
            imageLabel.setIcon(displayIcon);
            imageLabel.setText("");
        });
    }

    // ==================== RECEIVE VOICE ====================
    private void receiveVoice() throws IOException {
        int length = dis.readInt();
        byte[] audioData = readBytes(length);

        log("[VOICE] Received voice message (" + length + " bytes)");
        lastReceivedAudio = audioData;

        String fileName = DIR_VOICE + File.separator + "voice_" + System.currentTimeMillis() + ".wav";
        saveWav(audioData, fileName);
        log("[VOICE] Saved to: " + fileName);

        SwingUtilities.invokeLater(() -> {
            playVoiceBtn.setEnabled(true);
            voiceStatusLabel.setText("Pesan suara baru diterima!");
            voiceStatusLabel.setForeground(new Color(0, 150, 0));
        });

        playAudio(audioData);
    }

    // ==================== RECEIVE MESSAGE ====================
    private void receiveMessage() throws IOException {
        String message = dis.readUTF();
        String clientInfo = clientSocket.getInetAddress().getHostAddress()
                + ":" + clientSocket.getPort();
        log("[MESSAGE] " + clientInfo + " >> " + message);
    }

    // ==================== SEND IMAGE ====================
    private void sendImage() {
        if (clientSocket == null || clientSocket.isClosed())
            return;

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Pilih Gambar");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image files", "jpg", "jpeg", "png", "gif", "bmp"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            new Thread(() -> {
                try {
                    byte[] data = readFileBytes(file);
                    dos.writeUTF("IMAGE");
                    dos.writeUTF(file.getName());
                    dos.writeInt(data.length);
                    dos.write(data);
                    dos.flush();
                    log("[IMAGE] Sent: " + file.getName() + " (" + data.length + " bytes)");
                } catch (IOException ioe) {
                    log("Error sending image: " + ioe.getMessage());
                }
            }).start();
        }
    }

    // ==================== VOICE RECORD & SEND ====================
    private void startRecording() {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(audioFormat);
            microphone.start();
            isRecording = true;
            audioBuffer = new ByteArrayOutputStream();

            SwingUtilities.invokeLater(() -> {
                recordBtn.setEnabled(false);
                stopRecordBtn.setEnabled(true);
                sendVoiceBtn.setEnabled(false);
                voiceStatusLabel.setText("RECORDING...");
                voiceStatusLabel.setForeground(Color.RED);
            });
            log("[VOICE] Recording started...");

            new Thread(() -> {
                byte[] buf = new byte[4096];
                while (isRecording) {
                    int n = microphone.read(buf, 0, buf.length);
                    if (n > 0)
                        audioBuffer.write(buf, 0, n);
                }
            }).start();
        } catch (LineUnavailableException lue) {
            log("Error: " + lue.getMessage());
        }
    }

    private void stopRecording() {
        isRecording = false;
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
        lastRecordedAudio = audioBuffer.toByteArray();
        log("[VOICE] Recording stopped. Size: " + lastRecordedAudio.length + " bytes");

        SwingUtilities.invokeLater(() -> {
            recordBtn.setEnabled(true);
            stopRecordBtn.setEnabled(false);
            sendVoiceBtn.setEnabled(true);
            voiceStatusLabel.setText("Rekaman selesai. Siap dikirim.");
            voiceStatusLabel.setForeground(new Color(0, 100, 200));
        });
    }

    private void sendVoice() {
        if (lastRecordedAudio == null || clientSocket == null || clientSocket.isClosed())
            return;
        new Thread(() -> {
            try {
                dos.writeUTF("VOICE");
                dos.writeInt(lastRecordedAudio.length);
                dos.write(lastRecordedAudio);
                dos.flush();
                log("[VOICE] Sent to client (" + lastRecordedAudio.length + " bytes)");
            } catch (IOException ioe) {
                log("Error sending voice: " + ioe.getMessage());
            }
        }).start();
    }

    private void playReceivedAudio() {
        if (lastReceivedAudio != null)
            playAudio(lastReceivedAudio);
    }

    // ==================== UTILITY ====================
    private byte[] readBytes(int length) throws IOException {
        byte[] data = new byte[length];
        int totalRead = 0;
        while (totalRead < length) {
            int r = dis.read(data, totalRead, length - totalRead);
            if (r == -1)
                break;
            totalRead += r;
        }
        return data;
    }

    private byte[] readFileBytes(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        return data;
    }

    private Image scaleImage(Image img, int maxW, int maxH) {
        if (maxW <= 0 || maxH <= 0)
            return img;
        double scale = Math.min((double) maxW / img.getWidth(null),
                (double) maxH / img.getHeight(null));
        if (scale < 1.0) {
            return img.getScaledInstance((int) (img.getWidth(null) * scale),
                    (int) (img.getHeight(null) * scale),
                    Image.SCALE_SMOOTH);
        }
        return img;
    }

    private void playAudio(byte[] audioData) {
        new Thread(() -> {
            try {
                AudioInputStream ais = new AudioInputStream(
                        new ByteArrayInputStream(audioData), audioFormat,
                        audioData.length / audioFormat.getFrameSize());
                SourceDataLine speaker = (SourceDataLine) AudioSystem.getLine(
                        new DataLine.Info(SourceDataLine.class, audioFormat));
                speaker.open(audioFormat);
                speaker.start();
                log("[VOICE] Playing...");
                byte[] buf = new byte[4096];
                int n;
                while ((n = ais.read(buf)) != -1)
                    speaker.write(buf, 0, n);
                speaker.drain();
                speaker.close();
                log("[VOICE] Playback finished.");
            } catch (Exception e) {
                log("Playback error: " + e.getMessage());
            }
        }).start();
    }

    private void saveWav(byte[] data, String fileName) {
        try {
            AudioInputStream ais = new AudioInputStream(
                    new ByteArrayInputStream(data), audioFormat,
                    data.length / audioFormat.getFrameSize());
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(fileName));
        } catch (IOException e) {
            log("Error saving WAV: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UnifiedServer().setVisible(true));
    }
}
