import binascii
import random
import socket
import time

import message_data
from lab2.client import cache


class Client:

    def __init__(self):
        try:
            self.client_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            self.client_socket.connect(('8.8.8.8', 53))

        except TimeoutError:
            print("Сервер не отвечает")
            self.client_socket.close()

    def receive(self):
        """Метод получения данных от DNS-сервера"""
        while True:
            try:
                data, _ = self.client_socket.recvfrom(1024)
                message = message_data.parse_response(data)
                print(
                    f"Результатов по {message['QTYPE']}-запросу {message['QNAME']}: {int(message['ANCOUNT'], 2)}")
                for i in range(1, int(message['ANCOUNT'], 2) + 1):
                    print(f"{i}.\t{message['ANS'][i - 1]['RDATA']}")
                    cache.add(cache.CacheEntry(message['ANS'][i - 1]['TTL'], time.time(), message))

            except ConnectionAbortedError:
                break
            except Exception:
                self.client_socket.close()
                break

    def write(self):
        """Метод отправки DNS-запроса от клиента
           Запрос представляет собой шестнадцатеричную строку"""
        print("Usage: enter Resource Record Type (A, AAAA, MX, TXT) and URL")
        while True:
            try:
                RRType = input()
                if RRType in ["A", "AAAA", "MX", "TXT"]:
                    URL = input()
                    cache.update()
                    entry = cache.get(RRType, URL)
                    if entry:
                        print(
                            f"Кэш!\nРезультатов по {RRType}-запросу {URL}: {len(entry)}")
                        for i in range(1, len(entry) + 1):
                            print(f"{i}.\t{entry[i - 1]['RDATA']}")
                    else:
                        id = '{0:016b}'.format(random.getrandbits(16))
                        message = message_data.build_request(id, RRType, URL)
                        self.client_socket.send(binascii.unhexlify(message))
                else:
                    print("Incorrect type")
            except Exception:
                print("Сервер недоступен")
                break
