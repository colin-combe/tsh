/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package whose;

/**
 *
 * @author col
 */
public class PlaylistItem {
    int id;
    String title;
    String name;

    public PlaylistItem(int id, String title, String name) {
        this.id = id;
        this.title = title;
        this.name = name;
    }

    @Override
    public String toString() {
        return "id=" + id + ", title=" + title + ", name=" + name;
    }
    
}
