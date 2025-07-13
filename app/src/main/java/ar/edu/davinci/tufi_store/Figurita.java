package ar.edu.davinci.tufi_store;

public class Figurita {
    private String albumTitle;
    private String figuritaName;
    private String imageUrl;
    private double price; // Usamos double para el precio
    private String documentId; // para el ID del documento de Firestore

    // Constructor vacío requerido por Firestore para la deserialización
    public Figurita() {
    }

    // Constructor con todos los campos (se hizo para que no arroje error, pero se puede sacr)
    public Figurita(String albumTitle, String figuritaName, String imageUrl, double price) {
        this.albumTitle = albumTitle;
        this.figuritaName = figuritaName;
        this.imageUrl = imageUrl;
        this.price = price;
    }

    // Getters requeridos por Firestore para la serialización/deserialización
    public String getAlbumTitle() {
        return albumTitle;
    }

    public String getFiguritaName() {
        return figuritaName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public double getPrice() {
        return price;
    }

    public String getDocumentId() {
        return documentId;
    }

    // Setters - Firestore los usa si no tienes un constructor con todos los argumentos
    public void setAlbumTitle(String albumTitle) {
        this.albumTitle = albumTitle;
    }

    public void setFiguritaName(String figuritaName) {
        this.figuritaName = figuritaName;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

}