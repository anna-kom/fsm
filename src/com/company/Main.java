package com.company;

public class Main {

    public static void main(String[] args) {

        System.out.println("Исходный ДКА: (находится в файле fsm.txt)");
        FSM fsm = new FSM("fsm.txt");
        fsm.print();

        System.out.println("\n--------------------------------------------\n");
        System.out.println("Применим алгоритм минимизации");
        System.out.println();

        fsm.minimise(true);
    }
}
