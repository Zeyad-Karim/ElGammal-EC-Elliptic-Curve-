import java.util.*;

public class EC_Gamal {

    static class Point {
        int x, y;
        boolean isInfinity = false;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        static Point infinity() {
            Point p = new Point(0, 0);
            p.isInfinity = true;
            return p;
        }

        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Point p = (Point) obj;
            if (isInfinity && p.isInfinity) return true;
            if (isInfinity || p.isInfinity) return false;
            return x == p.x && y == p.y;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter a number to encrypt: ");
        while (!scanner.hasNextInt()) {
            System.out.println("Invalid input. Please enter a number:");
            scanner.next();
        }
        int message = scanner.nextInt();

        int p = getRandomPrime(2000);
        System.out.println("Chosen prime: " + p);

        int[] coeffs = chooseCoefficients(p);
        int a = coeffs[0], b = coeffs[1];
        System.out.println("Curve: y^2 = x^3 + " + a + "x + " + b + " mod " + p);

        Point G = findPrimitivePoint(a, b, p);
        System.out.println("Generator Point G: (" + G.x + "," + G.y + ")");

        int order = calculateGroupOrder(p);

        int aKey = new Random().nextInt(order - 1) + 1;
        int bKey = new Random().nextInt(order - 1) + 1;


        Point A = multiplyPoint(G, aKey, a, p);
        Point B = multiplyPoint(G, bKey, a, p);

        Point Tab_A = multiplyPoint(B, aKey, a, p);
        Point Tab_B = multiplyPoint(A, bKey, a, p);

        System.out.println("Public Key A: (" + A.x + "," + A.y + ")");
        System.out.println("Public Key B: (" + B.x + "," + B.y + ")");
        System.out.println("=============  key exchange check ============== ");
        System.out.println("Shared Tab for Sender (aB): (" + Tab_A.x + "," + Tab_A.y + ")");
        System.out.println("Shared Tab Reciver (bA):   (" + Tab_B.x + "," + Tab_B.y + ")");
        System.out.println("========================================================= ");
        int encrypted = encrypt(message, Tab_A, p);
        int decrypted = decrypt(encrypted, Tab_A, p);

        System.out.println("Encrypted Message: " + encrypted);
        System.out.println("Decrypted Message: " + decrypted);
    }

    public static boolean isPrime(int n) {
        if (n < 2) return false;
        for (int i = 2; i <= Math.sqrt(n); i++) {
            if (n % i == 0) return false;
        }
        return true;
    }

    public static int getRandomPrime(int min) {
        Random rand = new Random();
        int p;
        do {
            p = rand.nextInt(min * 2) + min;
        } while (!isPrime(p));
        return p;
    }

    private static int modPow(int base, int exponent, int modulus) {
        long result = 1;
        base %= modulus;
        while (exponent > 0) {
            if ((exponent & 1) == 1)
                result = (result * base) % modulus;
            base = (int)(((long)base * base) % modulus);
            exponent >>= 1;
        }
        return (int) result;
    }

    private static int[] chooseCoefficients(int p) {
        Random random = new Random();
        int a, b, discriminant;
        do {
            a = random.nextInt(p);
            b = random.nextInt(p);
            discriminant = (4 * modPow(a, 3, p) + 27 * modPow(b, 2, p)) % p;
        } while (discriminant == 0);
        return new int[]{a, b};
    }

    private static boolean isPointOnCurve(int x, int y, int a, int b, int p) {
        int left = (y * y) % p;
        int right = (modPow(x, 3, p) + a * x + b) % p;
        return left == (right + p) % p;
    }

    private static List<Point> getAllPoints(int a, int b, int p) {
        List<Point> points = new ArrayList<>();
        for (int x = 0; x < p; x++) {
            for (int y = 0; y < p; y++) {
                if (isPointOnCurve(x, y, a, b, p)) {
                    points.add(new Point(x, y));
                }
            }
        }
        return points;
    }

    private static Point findPrimitivePoint(int a, int b, int p) {
        List<Point> allPoints = getAllPoints(a, b, p);
        int groupOrder = allPoints.size() + 1; // +1 for point at infinity

        for (Point candidate : allPoints) {
            Set<String> generated = new HashSet<>();
            Point result = Point.infinity();
            for (int k = 1; k <= groupOrder; k++) {
                result = multiplyPoint(candidate, k, a, p);
                if (!result.isInfinity)
                    generated.add(result.x + "," + result.y);
            }

            if (generated.size() == allPoints.size()) {
                return candidate;
            }
        }
        return null;
    }

    private static int calculateGroupOrder(int p) {
        return (int)(p + 1 + 2 * Math.sqrt(p));
    }

    private static int modInverse(int a, int p) {
        a = ((a % p) + p) % p;
        for (int i = 1; i < p; i++) {
            if ((a * i) % p == 1) return i;
        }
        return -1;
    }

    private static Point addPoints(Point P, Point Q, int a, int p) {
        if (P.isInfinity) return Q;
        if (Q.isInfinity) return P;

        int S;
        if (P.equals(Q)) {
            int num = (3 * modPow(P.x, 2, p) + a) % p;
            int denom = modInverse(2 * P.y, p);
            S = (num * denom) % p;
        } else {
            if (P.x == Q.x) return Point.infinity();
            int num = (Q.y - P.y + p) % p;
            int denom = modInverse(Q.x - P.x + p, p);
            S = (num * denom) % p;
        }

        int xr = (modPow(S, 2, p) - P.x - Q.x + 2 * p) % p;
        int yr = (S * (P.x - xr + p) - P.y + p) % p;

        return new Point(xr, yr);
    }

    private static Point multiplyPoint(Point P, int k, int a, int p) {
        Point result = Point.infinity();
        Point addend = P;

        while (k > 0) {
            if ((k & 1) == 1) {
                result = addPoints(result, addend, a, p);
            }
            addend = addPoints(addend, addend, a, p);
            k >>= 1;
        }
        return result;
    }

    private static int encrypt(int message, Point tab, int p) {
        return (message * tab.x) % p;
    }

    private static int decrypt(int encrypted, Point tab, int p) {
        int inverse = modInverse(tab.x, p);
        return (encrypted * inverse) % p;
    }
}