package main;

import utils.DataBase;

public class Main {
    public static void main(String[] args) {
        try {
            System.out.println(DataBase.getConnection());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
