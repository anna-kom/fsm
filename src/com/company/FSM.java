package com.company;

import javafx.util.Pair;

import org.paukov.combinatorics3.Generator;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class FSM {

    ArrayList<String> states;                                       // Все состояния автомата
    ArrayList<String> alphabet;                                     // Алфавит
    HashMap<String, ArrayList<Pair<String, String>>> function;      // Функция переходов
    String startingState;                                           // Начальное состояние
    ArrayList<String> acceptingStates;                              // Допускающие (финальные) состояния

    public FSM(String fileName) {
        try {
            File file = new File(fileName);
            Scanner scanner = new Scanner(file);

            // Считываем начальное состояние
            if (scanner.hasNextLine()) {
                startingState = scanner.nextLine();
            }
            else {
                System.out.println("Ошибка: файл пустой");
                return;
            }

            // Считываем финальные состояния
            if (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                acceptingStates = new ArrayList<>();
                acceptingStates.addAll(Arrays.asList(line.split(" ")));
            }
            else {
                System.out.println("Ошибка: не указаны финальные состояния");
                return;
            }

            // Считываем алфавит
            if (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                alphabet = new ArrayList<>();
                alphabet.addAll(Arrays.asList(line.split(" ")));
            }
            else {
                System.out.println("Ошибка: не указан алфавит");
                return;
            }

            states = new ArrayList<>();
            function = new HashMap<>();
            // Считываем функцию переходов
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] possibleStates = line.split(" ");
                String state = possibleStates[0];
                states.add(state);
                ArrayList<Pair<String, String>> pairs = new ArrayList<>();
                for (int i = 0; i < alphabet.size(); i++) {
                    Pair<String, String> pair = new Pair<>(alphabet.get(i), possibleStates[i+1]);
                    pairs.add(pair);
                    function.put(state, pairs);
                }
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("Ошибка: файл не найден");
            e.printStackTrace();
        }
    }

    // Алгоритм минимизации (print - нужно ли печатать промежуточные вычисления)
    public void minimise(boolean print) {
        // Сначала удаляем недостижимые состояния
        deleteUnreachable();

        HashSet<ArrayList<String>> classes = new HashSet<>();
        HashSet<ArrayList<String>> finalClasses = new HashSet<>();
        ArrayList<String> Q01 = acceptingStates;
        ArrayList<String> Q02 = new ArrayList<>();
        for (String state: states) {
            if (!acceptingStates.contains(state)) {
                Q02.add(state);
            }
        }
        classes.add(Q01);
        classes.add(Q02);

        if (print) {
            System.out.println("Разбиваем на классы неразличимости\n");
            System.out.print("~0 : " + statesAsClass(Q01) + "   " + statesAsClass(Q02) + "\n");
        }

        int i = 1;
        do {
            HashSet<ArrayList<String>> newClasses = new HashSet<>();
            for (ArrayList<String> st : classes) {
                ArrayList<String> words = generateWords(i);
                HashMap<String, ArrayList<Pair<String, ArrayList<String>>>> stFunction = new HashMap<>();
                for (String q : st) {
                    ArrayList<Pair<String, ArrayList<String>>> pairs = new ArrayList<>();
                    for (String word : words) {
                        String state = q;
                        for (String symbol : word.split("")) {
                            state = nextState(state, symbol);
                        }
                        ArrayList<String> nextClass = new ArrayList<>();
                        for (ArrayList<String> cl : classes) {
                            if (cl.contains(state)) {
                                nextClass = new ArrayList<>(cl);
                            }
                        }
                        Pair<String, ArrayList<String>> pair = new Pair<>(word, nextClass);
                        pairs.add(pair);
                        stFunction.put(q, pairs);
                    }
                }

                for (Map.Entry<String, ArrayList<Pair<String, ArrayList<String>>>> current: stFunction.entrySet()) {
                    ArrayList<String> newClass = new ArrayList<>();
                    String currentKey = current.getKey();
                    newClass.add(currentKey);
                    for (Map.Entry<String, ArrayList<Pair<String, ArrayList<String>>>> other: stFunction.entrySet()) {
                        String otherKey = other.getKey();
                        if (!currentKey.equals(otherKey) && current.getValue().containsAll(other.getValue())
                            && other.getValue().containsAll(current.getValue())) {
                            newClass.add(otherKey);
                        }
                    }
                    Collections.sort(newClass);
                    newClasses.add(newClass);
                }
            }

            if (print) {
                System.out.println();
                System.out.print("~" + i + " : ");
                for (ArrayList<String> cl : newClasses) {
                    System.out.print(statesAsClass(cl) + "   ");
                }
                System.out.println();
            }

            for (ArrayList<String> cl : newClasses) {
                boolean found = false;
                Iterator<ArrayList<String>> iterator = classes.iterator();
                while (iterator.hasNext()) {
                    ArrayList<String> oldClass = iterator.next();
                    if (oldClass.containsAll(cl) && cl.containsAll(oldClass)) {
                        if (print) {
                            System.out.println("Класс " + statesAsClass(cl) + " стабилизировался");
                        }
                        iterator.remove();
                        finalClasses.add(cl);
                        found = true;
                    }
                }
                if (!found) {
                    classes.removeIf(oldClass -> oldClass.containsAll(cl));
                    classes.add(cl);
                }
            }
            i++;

        } while (!classes.isEmpty());

        ArrayList<String> newStartingState = new ArrayList<>();
        ArrayList<ArrayList<String>> newAcceptingStates = new ArrayList<>();
        for (ArrayList<String> st: finalClasses) {
            if (st.contains(startingState)) {
                newStartingState = st;
            }
            if (!Collections.disjoint(st, acceptingStates)) {
                newAcceptingStates.add(st);
            }
        }
        System.out.println();

        System.out.println("--------------------------------------------\n");
        System.out.println("Новое стартовое состояние: " + newStartingState);
        System.out.print("Новые финальные состояния: ");
        for (ArrayList<String> accState: newAcceptingStates) {
            System.out.print(accState + " ");
        }
        System.out.println();
        System.out.println("Новая функция переходов: ");
        System.out.print("          ");
        for (String a: alphabet) {
            System.out.print(a + "      ");
        }
        System.out.println();
        for (ArrayList<String> st: finalClasses) {
            System.out.print(st + " ");
            for (String symbol: alphabet) {
                String nextSt = nextState(st.get(0), symbol);
                ArrayList<String> cls = new ArrayList<>();
                for (ArrayList<String> fl: finalClasses) {
                    if (fl.contains(nextSt)) {
                        cls = fl;
                    }
                }
                System.out.print(cls + " ");
            }
            System.out.println();
        }
    }

    // Удаление недостижимых состояний
    public void deleteUnreachable() {
        HashSet<String> Q = new HashSet<>();
        Q.add(startingState);
        HashSet<String> newQ = new HashSet<>(Q);
        while (true) {
            for (String q : Q) {
                for (String symbol : alphabet) {
                    newQ.add(nextState(q, symbol));
                }
            }
            if (Q.containsAll(newQ)) {
                for (String state: states) {
                    if (!Q.contains(state)) {
                        function.remove(state);
                        states.remove(state);
                        acceptingStates.remove(state);
                    }
                }
                return;
            }
            Q.clear();
            Q = new HashSet<>(newQ);
        }
    }

    private String nextState(String state, String symbol) {
        int index = alphabet.indexOf(symbol);
        return function.get(state).get(index).getValue();
    }

    public void print() {
        System.out.println("Стартовое состояние: " + startingState);
        System.out.print("Финальные состояния: ");
        for (String accState: acceptingStates) {
            System.out.print(accState + " ");
        }
        System.out.println();
        System.out.println("Функция переходов: ");
        System.out.print("    ");
        for (String a: alphabet) {
            System.out.print(a + "   ");
        }
        System.out.println();
        for (String st: states) {
            System.out.print(st + "   ");
            for (String symbol: alphabet) {
                String nextSt = nextState(st, symbol);
                System.out.print(nextSt + "   ");
            }
            System.out.println();
        }
    }

    private ArrayList<String> generateWords(int length) {
        ArrayList<String> words = new ArrayList<>();
        for (List<String> comb: Generator.combination(alphabet).multi(length)) {
            for (List<String> perm: Generator.permutation(comb).simple()) {
                StringBuilder str = new StringBuilder();
                for (String s: perm) {
                    str.append(s);
                }
                words.add(str.toString());
            }
        }
        return words;
    }

    private StringBuilder statesAsClass(ArrayList<String> st) {
        StringBuilder res = new StringBuilder();
        res.append("[ ");
        for (String state: st) {
            res.append(state).append(", ");
        }
        res.delete(res.length() - 2, res.length() - 1);
        res.append("]");
        return res;
    }
}
