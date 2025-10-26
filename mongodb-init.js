const CONFIG = {
  database: 'global_mobility-apex-ecommerce',
  collections: {
    products: 'products',
    customers: 'customers',
    orders: 'orders'
  },
  validation: {
    level: 'strict',
    action: 'error'
  }
};

db = db.getSiblingDB(CONFIG.database);

const productsValidation = {
  $jsonSchema: {
    bsonType: "object",
    title: "Product Schema",
    required: ["productId", "name", "price", "stock", "active", "createdAt", "updatedAt"],
    additionalProperties: true,
    properties: {
      productId: {
        bsonType: "string",
        pattern: "^product-[0-9]+$",
        description: "Unique product identifier (format: product-N)"
      },
      name: {
        bsonType: "string",
        minLength: 3,
        maxLength: 200,
        description: "Product name (3-200 chars)"
      },
      description: {
        bsonType: "string",
        maxLength: 2000,
        description: "Detailed product description"
      },
      category: {
        bsonType: "string",
        minLength: 2,
        maxLength: 100,
        description: "Product category"
      },
      price: {
        bsonType: "double",
        minimum: 0,
        exclusiveMinimum: false,
        description: "Product price (must be >= 0)"
      },
      stock: {
        bsonType: "int",
        minimum: 0,
        description: "Available stock quantity (must be >= 0)"
      },
      active: {
        bsonType: "bool",
        description: "Product availability status"
      },
      createdAt: {
        bsonType: "date",
        description: "Record creation timestamp"
      },
      updatedAt: {
        bsonType: "date",
        description: "Last update timestamp"
      }
    }
  }
};

db.createCollection(CONFIG.collections.products, {
  validator: productsValidation,
  validationLevel: CONFIG.validation.level,
  validationAction: CONFIG.validation.action
});

db.products.createIndex(
  { "productId": 1 },
  {
    unique: true,
    name: "idx_products_productId_unique",
    background: false
  }
);

db.products.createIndex(
  { "active": 1, "stock": -1 },
  {
    name: "idx_products_active_stock",
    background: false
  }
);

db.products.createIndex(
  { "category": 1, "price": 1 },
  {
    name: "idx_products_category_price",
    background: false
  }
);

db.products.createIndex(
  { "name": "text", "description": "text" },
  {
    name: "idx_products_text_search",
    weights: { name: 10, description: 5 },
    default_language: "spanish"
  }
);

db.products.createIndex(
  { "createdAt": -1 },
  {
    name: "idx_products_created_desc",
    background: false
  }
);

db.products.createIndex(
  { "updatedAt": -1 },
  {
    name: "idx_products_updated_desc",
    background: false
  }
);

const customersValidation = {
  $jsonSchema: {
    bsonType: "object",
    title: "Customer Schema",
    required: ["customerId", "name", "email", "active", "createdAt", "updatedAt"],
    additionalProperties: true,
    properties: {
      customerId: {
        bsonType: "string",
        pattern: "^customer-[0-9]+$",
        description: "Unique customer identifier (format: customer-N)"
      },
      name: {
        bsonType: "string",
        minLength: 2,
        maxLength: 200,
        description: "Customer full name (2-200 chars)"
      },
      email: {
        bsonType: "string",
        pattern: "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
        description: "Valid email address"
      },
      phone: {
        bsonType: "string",
        description: "Contact phone number"
      },
      active: {
        bsonType: "bool",
        description: "Customer active status"
      },
      createdAt: {
        bsonType: "date",
        description: "Record creation timestamp"
      },
      updatedAt: {
        bsonType: "date",
        description: "Last update timestamp"
      }
    }
  }
};

db.createCollection(CONFIG.collections.customers, {
  validator: customersValidation,
  validationLevel: CONFIG.validation.level,
  validationAction: CONFIG.validation.action
});

db.customers.createIndex(
  { "customerId": 1 },
  {
    unique: true,
    name: "idx_customers_customerId_unique",
    background: false
  }
);

db.customers.createIndex(
  { "email": 1 },
  {
    unique: true,
    name: "idx_customers_email_unique",
    background: false
  }
);

db.customers.createIndex(
  { "active": 1 },
  {
    name: "idx_customers_active",
    background: false
  }
);

db.customers.createIndex(
  { "name": "text", "email": "text" },
  {
    name: "idx_customers_text_search",
    weights: { name: 10, email: 5 },
    default_language: "spanish"
  }
);

db.customers.createIndex(
  { "createdAt": -1 },
  {
    name: "idx_customers_created_desc",
    background: false
  }
);

db.customers.createIndex(
  { "updatedAt": -1 },
  {
    name: "idx_customers_updated_desc",
    background: false
  }
);

const ordersValidation = {
  $jsonSchema: {
    bsonType: "object",
    title: "Order Schema",
    required: ["orderId", "customerId", "customerName", "items", "totalAmount", "status", "createdAt", "updatedAt"],
    additionalProperties: true,
    properties: {
      orderId: {
        bsonType: "string",
        pattern: "^order-[0-9]+$",
        description: "Unique order identifier (format: order-N)"
      },
      customerId: {
        bsonType: "string",
        description: "Reference to customer ID"
      },
      customerName: {
        bsonType: "string",
        description: "Customer name snapshot"
      },
      customerEmail: {
        bsonType: "string",
        description: "Customer email snapshot"
      },
      items: {
        bsonType: "array",
        minItems: 1,
        description: "Order line items",
        items: {
          bsonType: "object",
          required: ["productId", "name", "price", "quantity"],
          properties: {
            productId: {
              bsonType: "string",
              description: "Product identifier"
            },
            name: {
              bsonType: "string",
              description: "Product name snapshot"
            },
            description: {
              bsonType: "string",
              description: "Product description snapshot"
            },
            price: {
              bsonType: "double",
              minimum: 0,
              description: "Unit price at time of order"
            },
            quantity: {
              bsonType: "int",
              minimum: 1,
              description: "Quantity ordered"
            },
            subtotal: {
              bsonType: "double",
              minimum: 0,
              description: "Line total (price * quantity)"
            }
          }
        }
      },
      totalAmount: {
        bsonType: "double",
        minimum: 0,
        description: "Total order amount"
      },
      status: {
        enum: ["PENDING", "PROCESSING", "COMPLETED", "FAILED", "CANCELLED"],
        description: "Order status"
      },
      createdAt: {
        bsonType: "date",
        description: "Order creation timestamp"
      },
      updatedAt: {
        bsonType: "date",
        description: "Last update timestamp"
      }
    }
  }
};

db.createCollection(CONFIG.collections.orders, {
  validator: ordersValidation,
  validationLevel: CONFIG.validation.level,
  validationAction: CONFIG.validation.action
});

db.orders.createIndex(
  { "orderId": 1 },
  {
    unique: true,
    name: "idx_orders_orderId_unique",
    background: false
  }
);

db.orders.createIndex(
  { "customerId": 1, "createdAt": -1 },
  {
    name: "idx_orders_customer_created",
    background: false
  }
);

db.orders.createIndex(
  { "status": 1, "createdAt": -1 },
  {
    name: "idx_orders_status_created",
    background: false
  }
);

db.orders.createIndex(
  { "items.productId": 1 },
  {
    name: "idx_orders_items_productId",
    background: false
  }
);

db.orders.createIndex(
  { "status": 1, "totalAmount": -1 },
  {
    name: "idx_orders_status_amount",
    background: false
  }
);

db.orders.createIndex(
  { "createdAt": -1 },
  {
    name: "idx_orders_created_desc",
    background: false
  }
);

db.orders.createIndex(
  { "updatedAt": -1 },
  {
    name: "idx_orders_updated_desc",
    background: false
  }
);

db.orders.createIndex(
  { "totalAmount": -1 },
  {
    name: "idx_orders_totalAmount_desc",
    background: false
  }
);

db.orders.createIndex(
  { "customerEmail": 1 },
  {
    name: "idx_orders_customerEmail",
    background: false
  }
);

const now = new Date();

const productsData = [
  {
    productId: "product-1",
    name: "Laptop HP Pavilion 15",
    description: "Laptop empresarial de alta gama con procesador Intel i7, 16GB RAM, 512GB SSD",
    category: "Electronics",
    price: 999.99,
    stock: 10,
    active: true,
    createdAt: now,
    updatedAt: now
  },
  {
    productId: "product-2",
    name: "Mouse Logitech MX Master 3",
    description: "Mouse inalámbrico ergonómico de precisión para profesionales",
    category: "Accessories",
    price: 29.99,
    stock: 50,
    active: true,
    createdAt: now,
    updatedAt: now
  },
  {
    productId: "product-3",
    name: "Teclado Mecánico Corsair K95 RGB",
    description: "Teclado mecánico gaming con iluminación RGB y switches Cherry MX",
    category: "Accessories",
    price: 79.99,
    stock: 25,
    active: true,
    createdAt: now,
    updatedAt: now
  }
];

db.products.insertMany(productsData);

const customersData = [
  {
    customerId: "customer-1",
    name: "Juan Pérez García",
    email: "juan.perez@example.com",
    phone: "+51 987654321",
    active: true,
    createdAt: now,
    updatedAt: now
  },
  {
    customerId: "customer-2",
    name: "María García López",
    email: "maria.garcia@example.com",
    phone: "+51 987654322",
    active: true,
    createdAt: now,
    updatedAt: now
  },
  {
    customerId: "customer-3",
    name: "Pedro López Martínez",
    email: "pedro.lopez@example.com",
    phone: "+51 987654323",
    active: false,
    createdAt: now,
    updatedAt: now
  }
];

db.customers.insertMany(customersData);