package storage.model.MyDoublyLinkedList;

public class MyDoubleLinkedList {

    private Node head;
    private Node tail;

    public MyDoubleLinkedList(Node head) {
        this.head = head;
        this.tail = head;
        if (head != null) {
            head.prev = null;
            head.next = null;
        }
    }

    public MyDoubleLinkedList() {
        this.head = null;
        this.tail = null;
    }

    public Node insert(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("Cannot insert null node");
        }
        if (head == null) {
            head = tail = node;
            tail.next = null;
            return node;
        }
        tail.next = node;
        node.prev = tail;
        tail = node;
        tail.next = null;

        return node;
    }

    public Node remove(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("Cannot remove null node");
        }
        if (head == null)
            throw new IllegalStateException("The LinkedList is empty");
        if (head == node && tail == node) {
            head = tail = null;
            return node;
        }
        if (head == node) {
            head = head.next;
            head.prev = null;
            node.next = null;
            node.prev = null;
            return node;
        }
        if (tail == node) {
            tail.prev.next = null;
            tail = tail.prev;
            node.prev = null;
            return node;
        }
        Node prev = node.prev;
        Node next = node.next;
        prev.next = next;
        next.prev = prev;
        node.next = null;
        node.prev = null;
        return node;

    }

    public String getLastInsertedKey() {
        return this.tail.key;
    }

}
