package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"time"

	"github.com/gorilla/mux"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

type Product struct {
	ProductID   string    `json:"productId" bson:"productId"`
	Name        string    `json:"name" bson:"name"`
	Description string    `json:"description" bson:"description"`
	Category    string    `json:"category" bson:"category"`
	Price       float64   `json:"price" bson:"price"`
	Stock       int       `json:"stock" bson:"stock"`
	Active      bool      `json:"active" bson:"active"`
	CreatedAt   time.Time `json:"createdAt" bson:"createdAt"`
	UpdatedAt   time.Time `json:"updatedAt" bson:"updatedAt"`
}

type Customer struct {
	CustomerID string    `json:"customerId" bson:"customerId"`
	Name       string    `json:"name" bson:"name"`
	Email      string    `json:"email" bson:"email"`
	Phone      string    `json:"phone" bson:"phone"`
	Active     bool      `json:"active" bson:"active"`
	CreatedAt  time.Time `json:"createdAt" bson:"createdAt"`
	UpdatedAt  time.Time `json:"updatedAt" bson:"updatedAt"`
}

var (
	mongoClient         *mongo.Client
	productsCollection  *mongo.Collection
	customersCollection *mongo.Collection
)

const (
	mongoURI   = "mongodb://localhost:27017"
	database   = "global_mobility-apex-ecommerce"
	serverPort = ":8080"
	apiVersion = "1.0.0"
)

func connectMongoDB() {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	clientOptions := options.Client().ApplyURI(mongoURI)

	client, err := mongo.Connect(ctx, clientOptions)
	if err != nil {
		log.Fatal("Error conectando a MongoDB:", err)
	}

	err = client.Ping(ctx, nil)
	if err != nil {
		log.Fatal("No se pudo hacer ping a MongoDB:", err)
	}

	mongoClient = client
	productsCollection = client.Database(database).Collection("products")
	customersCollection = client.Database(database).Collection("customers")

	fmt.Println("Conectado a MongoDB")
	fmt.Printf("Database: %s\n", database)
}

func getProduct(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	productID := vars["id"]

	log.Printf("GET /api/products/%s", productID)

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	var product Product
	err := productsCollection.FindOne(ctx, bson.M{"productId": productID}).Decode(&product)

	if err != nil {
		if err == mongo.ErrNoDocuments {
			log.Printf("Producto no encontrado: %s", productID)
			w.Header().Set("Content-Type", "application/json")
			w.WriteHeader(http.StatusNotFound)
			json.NewEncoder(w).Encode(map[string]string{
				"error":   "Product not found",
				"message": fmt.Sprintf("Product with ID '%s' does not exist", productID),
			})
			return
		}

		log.Printf("Error consultando producto: %v", err)
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{
			"error":   "Database error",
			"message": "An error occurred while fetching the product",
		})
		return
	}

	log.Printf("Producto encontrado: %s - %s ($%.2f)", product.ProductID, product.Name, product.Price)
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(product)
}

func getCustomer(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	customerID := vars["id"]

	log.Printf("GET /api/customers/%s", customerID)

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	var customer Customer
	err := customersCollection.FindOne(ctx, bson.M{"customerId": customerID}).Decode(&customer)

	if err != nil {
		if err == mongo.ErrNoDocuments {
			log.Printf("Cliente no encontrado: %s", customerID)
			w.Header().Set("Content-Type", "application/json")
			w.WriteHeader(http.StatusNotFound)
			json.NewEncoder(w).Encode(map[string]string{
				"error":   "Customer not found",
				"message": fmt.Sprintf("Customer with ID '%s' does not exist", customerID),
			})
			return
		}

		log.Printf("Error consultando cliente: %v", err)
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{
			"error":   "Database error",
			"message": "An error occurred while fetching the customer",
		})
		return
	}

	log.Printf("Cliente encontrado: %s - %s (active: %v)", customer.CustomerID, customer.Name, customer.Active)
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(customer)
}

func main() {
	connectMongoDB()

	defer func() {
		if err := mongoClient.Disconnect(context.Background()); err != nil {
			log.Printf("Error desconectando MongoDB: %v", err)
		} else {
			fmt.Println("\nDesconectado de MongoDB")
		}
	}()

	fmt.Println("Configurando rutas")
	router := mux.NewRouter()

	router.HandleFunc("/api/products/{id}", getProduct).Methods("GET")
	router.HandleFunc("/api/customers/{id}", getCustomer).Methods("GET")

	fmt.Println("GET /api/products/{id}")
	fmt.Println("GET /api/customers/{id}")

	fmt.Println("Presiona Ctrl+C para detener el servidor")

	log.Fatal(http.ListenAndServe(serverPort, router))
}
