from datetime import datetime, timezone
import json
import random
import time
from confluent_kafka import SerializingProducer
from faker import Faker

fake = Faker()

def generate_sales_transaction():
    user = fake.simple_profile()

    return {
        "transactionId": fake.uuid4(),
        "productId": random.choice(['product_1', 'product_2', 'product_3', 'product_4', 'product_5', 'product_6', 'product_7', 'product_8', 'product_9', 'product_10']),
        "productName": random.choice(['laptop', 'mouse', 'keyboard', 'monitor', 'speaker', 'printer', 'scanner', 'projector', 'router', 'switch']),
        "productCategory": random.choice(['electronics', 'office', 'computer', 'accessories', 'beauty', 'storage', 'home', 'software', 'other']),
        "productPrice": round(random.uniform(10, 1000), 2),
        "productQuantity": random.randint(1, 100),
        "productBrand": random.choice(['apple', 'samsung', 'dell', 'hp', 'lenovo', 'microsoft', 'sony', 'logitech', 'canon', 'nikon']),
        "currency": random.choice(['USD', 'EUR', 'GBP', 'JPY', 'KRW', 'CNY', 'INR', 'BRL', 'MXN', 'ARS']),
        "customerId": user['username'],
        "transactionDate": datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%S.%f%z'),
        "paymentMethod": random.choice(['credit_card', 'debit_card', 'paypal', 'bank_transfer', 'cash']),
        "paymentStatus": random.choice(['success', 'failed', 'pending']),
    }

def delivery_report(err, msg):
    if err is not None:
        print(f'Message delivery failed: {err}')
    else:
        print(f'Message delivered to {msg.topic()} [{msg.partition()}] at offset {msg.offset()}')

def main():
    topic = 'financial_transactions'
    producer = SerializingProducer({
        'bootstrap.servers': 'localhost:9092',
    })

    current_time = datetime.now()

    while (datetime.now()-current_time).seconds < 120:
        try:
            transaction = generate_sales_transaction()
            transaction['totalAmount'] = round(transaction['productPrice'] * transaction['productQuantity'], 2)
            
            print(transaction)
            producer.produce(
                topic=topic,
                key=transaction['transactionId'],
                value=json.dumps(transaction),
                on_delivery=delivery_report
            )

            producer.poll(0)

            # Wait for 5 seconds before sending the next transaction
            time.sleep(5)

        except BufferError:
            print("Buffer is full! waiting for 1 second")
            time.sleep(1)

        except Exception as e:
            print(f"Error: {e}")

if __name__ == "__main__":
    main()