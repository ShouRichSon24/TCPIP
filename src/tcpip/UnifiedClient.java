import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Unified TCP Client - Gabungan Tugas No. 3, 4, 5
 * Fitur:
 * - Tab 1: Input & kirim data Mahasiswa (Object Serialization)
 * - Tab 2: Kirim & terima gambar
 * - Tab 3: Rekam & kirim pesan suara, terima & putar pesan suara
 */
public class UnifiedClient extends JFrame {

    private static final String SERVER_HOST = "10.105.244.223";
    private static final int SERVER_PORT = 5005;

    // Common
    private JLabel statusLabel;
    private JTextArea logArea;
    private JButton connectBtn;
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private boolean isConnected = false;

    // Tab Mahasiswa
    private JTextField nimField, namaField, asalField, kelasField;
    private JButton sendMhsBtn, clearMhsBtn;
    private JTextArea mhsHistoryArea;

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

    public UnifiedClient() {
        setTitle("Unified TCP Client");
        setSize(750, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        audioFormat = new AudioFormat(16000, 16, 1, true, false);
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(5, 5));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(8, 8, 8, 8));

        // === TOP ===
        JPanel topPanel = new JPanel(new BorderLayout(8, 0));
        statusLabel = new JLabel("  Status: Disconnected");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        statusLabel.setForeground(Color.RED);
        connectBtn = new JButton("Connect to Server");
        connectBtn.addActionListener(e -> connectToServer());
        topPanel.add(statusLabel, BorderLayout.CENTER);
        topPanel.add(connectBtn, BorderLayout.EAST);
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
        JPanel panel = new JPanel(new BorderLayout(5, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Form Input
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Input Data Mahasiswa"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        String[] labels = { "NIM:", "Nama:", "Asal:", "Kelas Praktikum:" };
        nimField = new JTextField(20);
        namaField = new JTextField(20);
        asalField = new JTextField(20);
        kelasField = new JTextField(20);
        JTextField[] fields = { nimField, namaField, asalField, kelasField };

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.weightx = 0;
            JLabel lbl = new JLabel(labels[i]);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            formPanel.add(lbl, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            fields[i].setFont(new Font("SansSerif", Font.PLAIN, 12));
            formPanel.add(fields[i], gbc);
        }

        // Buttons
        gbc.gridx = 1;
        gbc.gridy = labels.length;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        sendMhsBtn = new JButton("Kirim Data");
        clearMhsBtn = new JButton("Clear");
        sendMhsBtn.setEnabled(false);
        sendMhsBtn.addActionListener(e -> sendMahasiswa());
        clearMhsBtn.addActionListener(e -> clearMhsFields());
        btnPanel.add(clearMhsBtn);
        btnPanel.add(sendMhsBtn);
        formPanel.add(btnPanel, gbc);

        panel.add(formPanel, BorderLayout.NORTH);

        // History
        mhsHistoryArea = new JTextArea(8, 50);
        mhsHistoryArea.setEditable(false);
        mhsHistoryArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane histScroll = new JScrollPane(mhsHistoryArea);
        histScroll.setBorder(BorderFactory.createTitledBorder("Riwayat Pengiriman"));
        panel.add(histScroll, BorderLayout.CENTER);

        return panel;
    }

    // ==================== TAB IMAGE ====================
    private JPanel createImageTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        imagePanel = new JPanel(new BorderLayout());
        imagePanel.setBorder(BorderFactory.createTitledBorder("Gambar Diterima dari Server"));
        imagePanel.setPreferredSize(new Dimension(600, 300));
        imageLabel = new JLabel("Belum ada gambar", SwingConstants.CENTER);
        imageLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
        imageLabel.setForeground(Color.GRAY);
        imagePanel.add(imageLabel, BorderLayout.CENTER);
        panel.add(imagePanel, BorderLayout.CENTER);

        sendImageBtn = new JButton("Kirim Gambar ke Server");
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
        sendVoiceBtn = new JButton("Kirim Rekaman ke Server");
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

    // ==================== CONNECT ====================
    private void connectToServer() {
        if (isConnected)
            return;
        connectBtn.setEnabled(false);

        new Thread(() -> {
            try {
                socket = new Socket(SERVER_HOST, SERVER_PORT);
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());
                isConnected = true;

                updateStatus("Connected to " + SERVER_HOST + ":" + SERVER_PORT, new Color(0, 150, 0));
                log("Connected to server " + SERVER_HOST + ":" + SERVER_PORT);

                SwingUtilities.invokeLater(() -> {
                    sendMhsBtn.setEnabled(true);
                    sendImageBtn.setEnabled(true);
                    recordBtn.setEnabled(true);
                });

                // Thread untuk menerima pesan dari server
                receiveFromServer();

                // Disconnected
                isConnected = false;
                updateStatus("Disconnected", Color.RED);
                SwingUtilities.invokeLater(() -> {
                    sendMhsBtn.setEnabled(false);
                    sendImageBtn.setEnabled(false);
                    recordBtn.setEnabled(false);
                    sendVoiceBtn.setEnabled(false);
                    connectBtn.setEnabled(true);
                });
                log("Disconnected from server.");

            } catch (IOException ioe) {
                log("Error: " + ioe.getMessage());
                SwingUtilities.invokeLater(() -> connectBtn.setEnabled(true));
            }
        }).start();
    }

    private void updateStatus(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("  Status: " + text);
            statusLabel.setForeground(color);
        });
    }

    // ==================== RECEIVE FROM SERVER ====================
    private void receiveFromServer() {
        try {
            while (isConnected) {
                String type = dis.readUTF();

                switch (type) {
                    case "IMAGE":
                        receiveImage();
                        break;
                    case "VOICE":
                        receiveVoice();
                        break;
                    default:
                        log("Unknown message from server: " + type);
                }
            }
        } catch (EOFException eof) {
            // normal
        } catch (IOException ioe) {
            if (isConnected)
                log("Connection lost: " + ioe.getMessage());
        }
    }

    // ==================== SEND MAHASISWA ====================
    private void sendMahasiswa() {
        String nim = nimField.getText().trim();
        String nama = namaField.getText().trim();
        String asal = asalField.getText().trim();
        String kelas = kelasField.getText().trim();

        if (nim.isEmpty() || nama.isEmpty() || asal.isEmpty() || kelas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Semua field harus diisi!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        new Thread(() -> {
            try {
                Mahasiswa mhs = new Mahasiswa(nim, nama, asal, kelas);

                // Serialize Mahasiswa ke bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(mhs);
                oos.flush();
                byte[] data = baos.toByteArray();

                dos.writeUTF("MAHASISWA");
                dos.writeInt(data.length);
                dos.write(data);
                dos.flush();

                log("[MAHASISWA] Sent: " + mhs.toString());

                String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                SwingUtilities.invokeLater(() -> {
                    mhsHistoryArea.append("[" + time + "] " + mhs.toString() + "\n");
                    clearMhsFields();
                });

            } catch (IOException ioe) {
                log("Error sending data: " + ioe.getMessage());
            }
        }).start();
    }

    private void clearMhsFields() {
        nimField.setText("");
        namaField.setText("");
        asalField.setText("");
        kelasField.setText("");
        nimField.requestFocus();
    }

    // ==================== SEND IMAGE ====================
    private void sendImage() {
        if (!isConnected)
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

    private void receiveImage() throws IOException {
        String fileName = dis.readUTF();
        int length = dis.readInt();
        byte[] imageData = readBytes(length);

        log("[IMAGE] Received: " + fileName + " (" + length + " bytes)");

        String savePath = "client_" + fileName;
        FileOutputStream fos = new FileOutputStream(savePath);
        fos.write(imageData);
        fos.close();
        log("[IMAGE] Saved to: " + savePath);

        ImageIcon icon = new ImageIcon(imageData);
        Image img = scaleImage(icon.getImage(), imagePanel.getWidth() - 20, imagePanel.getHeight() - 40);
        final ImageIcon displayIcon = new ImageIcon(img);
        SwingUtilities.invokeLater(() -> {
            imageLabel.setIcon(displayIcon);
            imageLabel.setText("");
        });
    }

    // ==================== VOICE ====================
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
        if (lastRecordedAudio == null || !isConnected)
            return;
        new Thread(() -> {
            try {
                dos.writeUTF("VOICE");
                dos.writeInt(lastRecordedAudio.length);
                dos.write(lastRecordedAudio);
                dos.flush();
                log("[VOICE] Sent to server (" + lastRecordedAudio.length + " bytes)");
            } catch (IOException ioe) {
                log("Error sending voice: " + ioe.getMessage());
            }
        }).start();
    }

    private void receiveVoice() throws IOException {
        int length = dis.readInt();
        byte[] audioData = readBytes(length);

        log("[VOICE] Received from server (" + length + " bytes)");
        lastReceivedAudio = audioData;

        String fileName = "client_voice_" + System.currentTimeMillis() + ".wav";
        saveWav(audioData, fileName);
        log("[VOICE] Saved to: " + fileName);

        SwingUtilities.invokeLater(() -> {
            playVoiceBtn.setEnabled(true);
            voiceStatusLabel.setText("Pesan suara baru diterima!");
            voiceStatusLabel.setForeground(new Color(0, 150, 0));
        });

        playAudio(audioData);
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
        SwingUtilities.invokeLater(() -> new UnifiedClient().setVisible(true));
    }
}
