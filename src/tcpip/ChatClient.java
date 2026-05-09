import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * TCP Chat Client (Non-GUI / Terminal Version)
 * Versi ringan untuk testing di terminal (Termux, SSH, dll).
 * Hanya fitur kirim-terima pesan teks.
 * 
 * Kompatibel dengan UnifiedServer (menggunakan protokol yang sama).
 * Pesan dikirim sebagai tipe "MESSAGE" via DataOutputStream.
 * 
 * Cara pakai:
 *   javac ChatClient.java
 *   java ChatClient <IP_SERVER>
 *   java ChatClient              (default: localhost)
 */
public class ChatClient {

    private static final int SERVER_PORT = 5005;

    public static void main(String[] args) {
        String host = (args.length > 0) ? args[0] : "localhost";

        System.out.println("==========================================");
        System.out.println("    TCP Chat Client (Terminal Version)");
        System.out.println("==========================================");
        System.out.println("Connecting to " + host + ":" + SERVER_PORT + "...");

        try {
            Socket socket = new Socket(host, SERVER_PORT);
            System.out.println("Connected!\n");

            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            Scanner scanner = new Scanner(System.in);

            // Thread untuk menerima pesan dari server
            Thread receiveThread = new Thread(() -> {
                try {
                    while (true) {
                        String type = dis.readUTF();
                        if (type.equals("MESSAGE")) {
                            String msg = dis.readUTF();
                            System.out.println("\n[Server] " + msg);
                            System.out.print(">> ");
                        } else {
                            // Skip data dari tipe lain (IMAGE/VOICE)
                            if (type.equals("IMAGE")) {
                                dis.readUTF(); // filename
                            }
                            int len = dis.readInt();
                            dis.skipBytes(len);
                            System.out.println("\n[Info] Received " + type + " (skipped, terminal mode)");
                            System.out.print(">> ");
                        }
                    }
                } catch (EOFException eof) {
                    System.out.println("\nServer disconnected.");
                } catch (IOException ioe) {
                    System.out.println("\nConnection lost: " + ioe.getMessage());
                }
            });
            receiveThread.setDaemon(true);
            receiveThread.start();

            // Loop kirim pesan
            System.out.println("Ketik pesan lalu Enter. Ketik 'exit' untuk keluar.\n");
            while (true) {
                System.out.print(">> ");
                String message = scanner.nextLine().trim();

                if (message.equalsIgnoreCase("exit")) {
                    break;
                }
                if (message.isEmpty()) {
                    continue;
                }

                dos.writeUTF("MESSAGE");
                dos.writeUTF(message);
                dos.flush();
            }

            System.out.println("Disconnected.");
            scanner.close();
            socket.close();

        } catch (ConnectException ce) {
            System.err.println("ERROR: Cannot connect to " + host + ":" + SERVER_PORT);
            System.err.println("Pastikan server sudah berjalan!");
        } catch (IOException ioe) {
            System.err.println("Error: " + ioe.getMessage());
        }
    }
}
