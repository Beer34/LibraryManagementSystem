import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

// --- Custom Exceptions ---

class ItemNotFoundException extends Exception {
    public ItemNotFoundException(String message) {
        super(message);
    }
}

class InvalidLoanPeriodException extends RuntimeException {
    public InvalidLoanPeriodException(String message) {
        super(message);
    }
}

// --- Custom Immutable Type (ISBN) ---

final class ISBN {
    private final String code;

    public ISBN(String code) {
        if (code == null) {
            throw new IllegalArgumentException("ISBN code cannot be null.");
        }
        
        String cleanedCode = code.trim().replace("-", "");

        if (cleanedCode.length() != 13) {
            throw new IllegalArgumentException("ISBN must contain 13 numerical characters after removing hyphens.");
        }
        
        this.code = cleanedCode;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return "ISBN: " + code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ISBN isbn = (ISBN) o;
        return Objects.equals(code, isbn.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}

// --- Enums ---

enum LoanStatus {
    ACTIVE,
    RETURNED,
    OVERDUE
}

enum MemberType {
    STUDENT,
    FACULTY,
    GUEST
}

// --- Sealed Interface and Base Class ---

sealed interface LibraryItem permits AbstractLibraryItem {
    String getTitle();
    ISBN getIsbn();
    String getDetails();
    boolean isCheckedOut();

    default void checkIn() {
        System.out.println(getTitle() + " has been checked in successfully.");
    }
    
    static String getPolicy() {
        return "Standard loan period is 14 days.";
    }

    private boolean isAvailableForLoan() {
        return true;
    }
}

non-sealed abstract class AbstractLibraryItem implements LibraryItem {
    private final String title;
    private final ISBN isbn;
    private boolean isCheckedOut = false;

    public AbstractLibraryItem(String title, ISBN isbn) {
        this.title = title;
        this.isbn = isbn;
    }

    @Override
    public String getTitle() { return title; }

    @Override
    public ISBN getIsbn() { return isbn; }
    
    public void setCheckedOut(boolean checkedOut) {
        this.isCheckedOut = checkedOut;
    }

    @Override
    public boolean isCheckedOut() {
        return isCheckedOut;
    }
}

// --- Implementing Classes ---

final class Book extends AbstractLibraryItem {
    private final String author;
    private int numberOfCopies;

    public Book(String title, ISBN isbn, String author) {
        this(title, isbn, author, 1);
    }

    public Book(String title, ISBN isbn, String author, int copies) {
        super(title, isbn);
        this.author = author;
        this.numberOfCopies = copies;
    }

    public String getAuthor() { return author; }
    public int getNumberOfCopies() { return numberOfCopies; }

    public void addCopies(int count) {
        this.numberOfCopies += count;
    }

    public void addCopies() {
        addCopies(1);
    }

    @Override
    public String getDetails() {
        return String.format("Book: '%s' by %s. Copies: %d. %s",
                getTitle(), author, numberOfCopies, getIsbn().toString());
    }
}

final class Journal extends AbstractLibraryItem {
    private final int volume;
    private final int issue;

    public Journal(String title, ISBN isbn, int volume, int issue) {
        super(title, isbn);
        this.volume = volume;
        this.issue = issue;
    }

    @Override
    public String getDetails() {
        return String.format("Journal: '%s', Vol %d, Issue %d. %s",
                getTitle(), volume, issue, getIsbn().toString());
    }
}

// --- Library Member ---

class LibraryMember {
    private final String memberId;
    private String name;
    private MemberType type;

    public LibraryMember(String name, MemberType type) {
        var uniqueId = UUID.randomUUID().toString().substring(0, 8);
        this.memberId = uniqueId;
        this.name = name;
        this.type = type;
    }

    public String getMemberId() { return memberId; }
    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public MemberType getType() { return type; }

    public String getDetails() {
        StringBuilder sb = new StringBuilder();
        sb.append("Member: ").append(name);
        sb.append(" (ID: ").append(memberId).append(")");
        sb.append(" - Type: ").append(type);
        return sb.toString();
    }
}

// --- Loan Record ---

record Loan(
    String loanId,
    LibraryItem item,
    LibraryMember member,
    LocalDate loanDate,
    LocalDate dueDate,
    LoanStatus status
) {
    public Loan {
        if (loanDate.isAfter(dueDate)) {
            throw new InvalidLoanPeriodException("Due date cannot be before loan date.");
        }
    }

    public Loan returnItem() {
        return new Loan(loanId, item, member, loanDate, dueDate, LoanStatus.RETURNED);
    }
}

// --- Searchable Interface ---

interface Searchable {
    boolean matchesSearch(String query);
}

// --- Library Core Management Class ---

class Library implements Searchable {
    private final List<LibraryItem> items;
    private final List<LibraryMember> members;
    private final List<Loan> loans;

    public Library() {
        this.items = new ArrayList<>();
        this.members = new ArrayList<>();
        this.loans = new ArrayList<>();
    }

    public List<LibraryItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public List<Loan> getLoans() {
        return Collections.unmodifiableList(loans);
    }

    public void replaceLoanForTesting(Loan oldLoan, Loan newLoan) {
        loans.remove(oldLoan);
        loans.add(newLoan);
    }
    
    public void addItem(Book book) {
        items.add(book);
    }

    public void addItem(Journal journal) {
        items.add(journal);
    }

    public void addMembers(LibraryMember... newMembers) {
        for (LibraryMember member : newMembers) {
            members.add(member);
        }
    }

    public Loan loanItem(LibraryItem item, LibraryMember member) {
        if (item.isCheckedOut()) {
            System.out.println("Item is currently checked out.");
            return null;
        }

        var loanDate = LocalDate.now();
        var dueDate = loanDate.plusDays(14);

        try {
            var newLoan = new Loan(
                UUID.randomUUID().toString(),
                item,
                member,
                loanDate,
                dueDate,
                LoanStatus.ACTIVE
            );

            loans.add(newLoan);
            if (item instanceof AbstractLibraryItem abstractItem) {
                abstractItem.setCheckedOut(true);
            }
            System.out.printf("%s loaned to %s. Due on %s.%n", item.getTitle(), member.getName(), dueDate);
            return newLoan;
        } catch (InvalidLoanPeriodException e) {
            System.err.println("Loan creation failed: " + e.getMessage());
            return null;
        }
    }

    public void returnItem(LibraryItem item) throws ItemNotFoundException {
        var foundLoan = loans.stream()
            .filter(loan -> loan.item().equals(item) && loan.status() == LoanStatus.ACTIVE)
            .findFirst();

        if (foundLoan.isEmpty()) {
            throw new ItemNotFoundException("Item not found on active loan records.");
        }

        var oldLoan = foundLoan.get();
        var newLoan = oldLoan.returnItem();

        loans.remove(oldLoan);
        loans.add(newLoan);

        if (item instanceof AbstractLibraryItem abstractItem) {
            abstractItem.setCheckedOut(false);
        }
        
        System.out.printf("%s returned by %s. Status: %s.%n", 
            item.getTitle(), oldLoan.member().getName(), newLoan.status());

        var daysOverdue = ChronoUnit.DAYS.between(oldLoan.dueDate(), LocalDate.now());
        if (daysOverdue > 0) {
            System.out.printf("Fine calculated: $%.2f%n", calculateFine(daysOverdue, oldLoan.member().getType()));
        }
    }

    public double calculateFine(long daysOverdue, MemberType type) {
        double rate = switch (type) {
            case STUDENT -> 0.10;
            case GUEST   -> 0.25;
            case FACULTY -> 0.00;
        };
        
        return (rate == 0.00) ? 0.00 : daysOverdue * rate;
    }

    public List<LibraryItem> searchItems(Predicate<LibraryItem> predicate) {
        System.out.println("--- ISBNs of all items via Method Reference ---");
        getItems().stream()
            .map(LibraryItem::getIsbn)
            .forEach(System.out::println);
        System.out.println("----------------------------------------------");

        var filteredList = items.stream()
            .filter(predicate)
            .toList();

        return filteredList;
    }

    @Override
    public boolean matchesSearch(String query) {
        return items.stream().anyMatch(item -> item.getTitle().contains(query));
    }
}

public class LibraryManagementSystem {

    public static void main(String[] args) {
        System.out.println("--- Library Management System (Java 21 LTS Demo) ---");

        var library = new Library();

        // --- Data Setup ---
        var isbn1 = new ISBN("978-0321356680");
        var isbn2 = new ISBN("978-1509897103");
        var isbn3 = new ISBN("978-0262510875");

        var cleanCode = new Book("Clean Code", isbn1, "Robert C. Martin");
        var journalOfCS = new Journal("Journal of Comp Sci", isbn3, 45, 1);
        var toKillAMockingbird = new Book("To Kill a Mockingbird", isbn2, "Harper Lee", 5);
        toKillAMockingbird.addCopies();

        library.addItem(cleanCode);
        library.addItem(toKillAMockingbird);
        library.addItem(journalOfCS);

        var alice = new LibraryMember("Alice Johnson", MemberType.STUDENT);
        var bob = new LibraryMember("Bob Williams", MemberType.FACULTY);
        library.addMembers(alice, bob);

        System.out.println("\n--- Initial Item & Member Details ---");
        library.getItems().forEach(item -> System.out.println(item.getDetails()));
        System.out.println(alice.getDetails());
        System.out.println(bob.getDetails());

        // --- Demo 1: Loan and Return ---
        System.out.println("\n--- Demo 1: Loan & Return ---");
        var loan1 = library.loanItem(cleanCode, alice);

        try {
            library.returnItem(journalOfCS); 
        } catch (ItemNotFoundException ignoredE) {
            System.err.println("Handled checked exception: " + ignoredE.getMessage());
        }

        System.out.println(LibraryItem.getPolicy());

        if (loan1 != null) {
            var overdueLoan = new Loan(
                loan1.loanId(), loan1.item(), loan1.member(), 
                loan1.loanDate().minusDays(20), 
                LocalDate.now().minusDays(6), 
                LoanStatus.ACTIVE
            );
            library.replaceLoanForTesting(loan1, overdueLoan);
        }

        try {
            library.returnItem(cleanCode);
        } catch (ItemNotFoundException ignoredE) {
            System.err.println("Error during return: " + ignoredE.getMessage());
        }

        // --- Demo 2: Predicate Lambda and Filtering ---
        System.out.println("\n--- Demo 2: Predicate Lambda Search ---");
        
        ISBN targetIsbn = isbn1;
        Predicate<LibraryItem> isCleanCode = item -> item.getIsbn().equals(targetIsbn);

        var searchResults = library.searchItems(isCleanCode);

        System.out.println("Search results for Clean Code:");
        searchResults.forEach(item -> System.out.println(item.getDetails()));

        // --- Demo 3: Final Checks and Java 22/23 Feature Mention ---
        System.out.println("\n--- Final Loan Status ---");
        library.getLoans().forEach(loan -> System.out.println(loan));

        System.out.println("\n--- Java 22/23 Feature: Unnamed Variables ---");
        System.out.println("In Java 22+, you could use an unnamed variable '_' in a catch block or lambda if the variable isn't used:");
        try {
            throw new ItemNotFoundException("Test");
        } catch (ItemNotFoundException ignoredE) {
            System.out.println("Caught exception but ignored the detail with a non-used variable name.");
        }
        
        String title = cleanCode.getTitle();
        String info = "The title is " + title + " and its ISBN is " + isbn1.getCode();
        System.out.println(info);
    }
}
