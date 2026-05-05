import java.io.Serializable;

/**
 * Tugas No. 3 - Kelas Mahasiswa (Serializable)
 * Kelas ini merepresentasikan data mahasiswa yang dapat
 * dikirim melalui jaringan menggunakan Object Serialization.
 */
public class Mahasiswa implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nim;
    private String nama;
    private String asal;
    private String kelasPraktikum;

    public Mahasiswa(String nim, String nama, String asal, String kelasPraktikum) {
        this.nim = nim;
        this.nama = nama;
        this.asal = asal;
        this.kelasPraktikum = kelasPraktikum;
    }

    public String getNim() {
        return nim;
    }

    public String getNama() {
        return nama;
    }

    public String getAsal() {
        return asal;
    }

    public String getKelasPraktikum() {
        return kelasPraktikum;
    }

    @Override
    public String toString() {
        return "NIM: " + nim + " | Nama: " + nama + " | Asal: " + asal + " | Kelas: " + kelasPraktikum;
    }
}
