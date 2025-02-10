import java.io.*;
import java.util.*;

class Node implements Comparable<Node> {
    char ch;
    int frequency;
    Node left, right;

    Node(char ch, int frequency) {
        this.ch = ch;
        this.frequency = frequency;
    }

    Node(int frequency, Node left, Node right) {
        this.frequency = frequency;
        this.left = left;
        this.right = right;
    }

    @Override
    public int compareTo(Node other) {
        return this.frequency - other.frequency;
    }
}

public class HuffmanCompression {

    private static Map<Character, String> huffmanCodes = new HashMap<>();
    private static Node root;

    public static void main(String[] args) throws IOException {
        String inputFilePath = "input.txt";
        String compressedFilePath = "compressed.bin";
        String decompressedFilePath = "decompressed.txt";

        String text = readFile(inputFilePath);

        // Step 1: Build Frequency Map
        Map<Character, Integer> frequencyMap = buildFrequencyMap(text);

        // Step 2: Build Huffman Tree
        root = buildHuffmanTree(frequencyMap);

        // Step 3: Generate Huffman Codes
        generateCodes(root, "");

        // Step 4: Compress the File
        compressFile(text, compressedFilePath);

        // Step 5: Decompress the File
        decompressFile(compressedFilePath, decompressedFilePath);
    }

    private static String readFile(String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private static Map<Character, Integer> buildFrequencyMap(String text) {
        Map<Character, Integer> frequencyMap = new HashMap<>();
        for (char ch : text.toCharArray()) {
            frequencyMap.put(ch, frequencyMap.getOrDefault(ch, 0) + 1);
        }
        return frequencyMap;
    }

    private static Node buildHuffmanTree(Map<Character, Integer> frequencyMap) {
        PriorityQueue<Node> pq = new PriorityQueue<>();
        for (Map.Entry<Character, Integer> entry : frequencyMap.entrySet()) {
            pq.add(new Node(entry.getKey(), entry.getValue()));
        }

        while (pq.size() > 1) {
            Node left = pq.poll();
            Node right = pq.poll();
            pq.add(new Node(left.frequency + right.frequency, left, right));
        }

        return pq.poll();
    }

    private static void generateCodes(Node node, String code) {
        if (node == null) return;

        if (node.left == null && node.right == null) {
            huffmanCodes.put(node.ch, code);
        }

        generateCodes(node.left, code + "0");
        generateCodes(node.right, code + "1");
    }

    private static void compressFile(String text, String outputFilePath) throws IOException {
        StringBuilder encodedText = new StringBuilder();
        for (char ch : text.toCharArray()) {
            encodedText.append(huffmanCodes.get(ch));
        }

        try (BitOutputStream bos = new BitOutputStream(new FileOutputStream(outputFilePath))) {
            for (char bit : encodedText.toString().toCharArray()) {
                bos.writeBit(bit - '0');
            }
        }
    }

    private static void decompressFile(String inputFilePath, String outputFilePath) throws IOException {
        StringBuilder decodedText = new StringBuilder();

        try (BitInputStream bis = new BitInputStream(new FileInputStream(inputFilePath))) {
            Node current = root;
            int bit;
            while ((bit = bis.readBit()) != -1) {
                current = (bit == 0) ? current.left : current.right;

                if (current.left == null && current.right == null) {
                    decodedText.append(current.ch);
                    current = root;
                }
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            writer.write(decodedText.toString());
        }
    }
}

class BitOutputStream implements AutoCloseable {
    private final OutputStream out;
    private int currentByte;
    private int numBitsFilled;

    public BitOutputStream(OutputStream out) {
        this.out = out;
        this.currentByte = 0;
        this.numBitsFilled = 0;
    }

    public void writeBit(int bit) throws IOException {
        if (bit != 0 && bit != 1) {
            throw new IllegalArgumentException("Bit must be 0 or 1");
        }
        currentByte = (currentByte << 1) | bit;
        numBitsFilled++;
        if (numBitsFilled == 8) {
            out.write(currentByte);
            currentByte = 0;
            numBitsFilled = 0;
        }
    }

    @Override
    public void close() throws IOException {
        if (numBitsFilled > 0) {
            currentByte <<= (8 - numBitsFilled);
            out.write(currentByte);
        }
        out.close();
    }
}

class BitInputStream implements AutoCloseable {
    private final InputStream in;
    private int currentByte;
    private int numBitsRemaining;

    public BitInputStream(InputStream in) {
        this.in = in;
        this.currentByte = 0;
        this.numBitsRemaining = 0;
    }

    public int readBit() throws IOException {
        if (numBitsRemaining == 0) {
            currentByte = in.read();
            if (currentByte == -1) return -1;
            numBitsRemaining = 8;
        }
        numBitsRemaining--;
        return (currentByte >> numBitsRemaining) & 1;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
