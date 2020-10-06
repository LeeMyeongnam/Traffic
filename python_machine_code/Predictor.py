import tensorflow as tf
import numpy as np
import csv
import os
import requests
import xml.etree.ElementTree as elemTree
import datetime
from datetime import datetime as dt
import time

road_directory_name = 'inputs'  # 머신러닝 학습에 사용되는 2018년 교통 소요 시간 데이터들이 저장되어 있는 폴더
model_directory_name = 'models'  # 머신러닝 학습의 결과 모델들이 저장되어 있는 폴더
predict_directory_name = 'predict_inputs/'  # API를 통해 실시간으 영업소 간 통행 소요 시간 데이터를 xml 형태로 받아와서 csv 파일로 저장할 폴더, 예측의 입력으로 사용됨
result_directory_name = 'data'  # 모델을 통해 예측된 결과들이 저장되는 폴더


# 너무 작거나 너무 큰 값이 학습을 방해하는 것을 방지하고자 정규화한다
# lst가 양수라는 가정하에 최소값과 최대값을 이용하여 0~1사이의 값으로 변환
# Min-Max scaling
def min_max_scaling(lst):
    lst_np = np.asarray(lst)
    return (lst_np - lst_np.min()) / (lst_np.max() - lst_np.min() + 1e-7)  # 1e-7은 0으로 나누는 오류 예방차원


# 정규화된 값을 원래의 값으로 되돌린다
# 정규화하기 이전의 org_lst값과 되돌리고 싶은 lst를 입력하면 역정규화된 값을 리턴한다
def reverse_min_max_scaling(org_lst, lst):
    org_lst_np = np.asarray(org_lst)
    lst_np = np.asarray(lst)
    return (lst_np * (org_lst_np.max() - org_lst_np.min() + 1e-7)) + org_lst_np.min()


def predict(name):
    tf.reset_default_graph()  # tf를 초기화함, tf를 재사용하기 위해 사용

    seq_length = 24  # 최근 24개의 데이터를 입력으로 넣음
    data_dim = 7  # 월, 일, 휴일 여부, 요일 코드, 시간대, 강수량, 소요 시간
    hidden_dim = 16  # 예측 과정에서 16개의 레이어를 사용
    output_dim = 24  # 미래 24시간 동안의 데이터 출력

    X = tf.placeholder(tf.float32, [None, seq_length, data_dim])

    cell = tf.contrib.rnn.BasicLSTMCell(num_units=hidden_dim, state_is_tuple=True, activation=tf.tanh)
    outputs, _state = tf.nn.dynamic_rnn(cell, X, dtype=tf.float32)

    Y_pred = tf.contrib.layers.fully_connected(outputs[:, -1], output_dim, activation_fn=None)

    saver = tf.train.Saver()

    with tf.Session() as sess:
        saver.restore(sess, model_directory_name + '/' + name + '.ckpt')

        xy = np.genfromtxt(predict_directory_name + name + '.csv', delimiter=',')
        original_time = xy[:, 4]
        original_elapsed_time = xy[:, 6]

        # 예측에 사용될 데이터 정규화
        norm_month = min_max_scaling(xy[:, 0])
        norm_day = min_max_scaling(xy[:, 1])
        norm_holiday = min_max_scaling(xy[:, 2])
        norm_weekday = min_max_scaling(xy[:, 3])
        norm_time = min_max_scaling(xy[:, 4])
        norm_rain = min_max_scaling(xy[:, 5])
        norm_elapsed_time = min_max_scaling(xy[:, 6])

        factorX = []
        for i in range(0, len(norm_holiday)):
            factorX.append((norm_month[i], norm_day[i], norm_holiday[i], norm_weekday[i], norm_time[i], norm_rain[i], norm_elapsed_time[i]))
        factorX = np.array(factorX)

        factorX = factorX.reshape(1, 24, 7)
        test_predict = sess.run(Y_pred, feed_dict={X: factorX})

        data = []
        for i in range(0, output_dim):
            congestion = (reverse_min_max_scaling(original_elapsed_time, test_predict[0][i]) - sum(original_elapsed_time)/len(original_elapsed_time))/(reverse_min_max_scaling(original_elapsed_time, test_predict[0][i]) + sum(original_elapsed_time)/len(original_elapsed_time))
            time = (int(original_time[output_dim - 1]) + 1 + i) % 24
            data.append((time, int(round(reverse_min_max_scaling(original_elapsed_time, test_predict[0][i]))), round(congestion, 2)))

        with open(result_directory_name + '/' + name + '.csv', 'w', newline='') as f:
            wr = csv.writer(f)
            for row in data:
                wr.writerow(row)


def get_traffic_data(key, departure, arrival):
    # 실시간 영업소 간 통행 소요 시간 데이터 받아오기
    params = {'key': key,
              'type': 'xml',
              'iStartUnitCode': departure,
              'iEndUnitCode': arrival,
              'iStartEndStdTypeCode': '2',
              'sumTmUnitTypeCode': '2',
              'numOfRows': '100',
              'pageNo': '1'}
    response = requests.get(url='http://data.ex.co.kr/openapi/trtm/realUnitTrtm', params=params, timeout=600)
    xml_str = response.content.decode('utf-8')

    # xml 문자열 처리
    input_list = []
    tree = elemTree.fromstring(xml_str)
    count = int(tree.find('./count').text)
    if count < 24:
        return False

    # xml 데이터를 리스트에 삽입
    for data in tree.findall('./realUnitTrtmVO'):
        if data.find('./stdTime').text[3:5] == '00' or data.find('./stdTime').text[3:5] == '30':  # 0분 또는 30분 데이터만 추출하기
            date = data.find('./stdDate').text  # YYYYMMDD
            month = int(date[4:6])  # MM
            day = int(date[6:8])  # DD
            weekday = (datetime.date(int(date[0:4]), int(date[4:6]), int(date[6:8])).weekday() + 1) % 7 + 1  # 엑셀에서 제공하는 weekday 타입 맞추기(1:일, 2:월, ..., 7:토)
            if weekday == 1 or weekday == 7:  # 휴일 여부
                holiday = 1
            else:
                holiday = 0
            time = int(data.find('./stdTime').text[0:2]) + int(data.find('./stdTime').text[3:5])/60  # (x시 30분) 데이터는 (x.5시)로 변환
            rain = 0  # 강수량은 0으로 가정
            elapsed_time = round(float(data.find('./timeAvg').text)*60)  # 분 단위의 데이터를 초 단위로 바꾸기

            input_list.append((month, day, weekday, holiday, time, rain, elapsed_time))

    # 리스트 시간 순서대로 정렬
    for i in range(0, len(input_list)):
        for j in range(i+1, len(input_list)):
            if input_list[i][4] > input_list[j][4]:
                temp = input_list[j]
                input_list[j] = input_list[i]
                input_list[i] = temp

    # 중복된 시간 있으면 앞의 것 제거
    i = 0
    while True:
        if i >= len(input_list)-1:
            break
        if input_list[i][4] == input_list[i+1][4]:
            input_list.remove(input_list[i])
            i = 0
        else:
            i = i + 1

    if len(input_list) < 24:
        return False

    file_name = str(departure) + "_" + str(arrival)

    with open(predict_directory_name + '/' + file_name + '.csv', 'w', newline='') as f:
        wr = csv.writer(f)
        for row in input_list[len(input_list)-24:len(input_list)]:  # 데이터 최신순 24개 넣음
            wr.writerow(row)

    return True


def main_function(key):
    file_list = os.listdir(road_directory_name)
    while True:
        exception_occurred = False
        start_time = time.time()
        for step in range(0, len(file_list)):
            file_name = file_list[step][0:-4]
            request_result = False
            try:
                request_result = get_traffic_data(key, file_name[0:3], file_name[4:7])
            except requests.exceptions.ReadTimeout:
                exception_occurred = True
                print("request timeout exception 발생, 30초 뒤 재요청")
                time.sleep(30)
            except requests.exceptions.ConnectionError:
                exception_occurred = True
                print("Connection aborted exception 발생, 30초 뒤 재요청")
                time.sleep(30)
            except ConnectionResetError:
                exception_occurred = True
                print("ConnectionResetError 발생, 30초 뒤 재요청")
                time.sleep(30)
            except Exception:
                exception_occurred = True
                print("Exception 발생, 30초 뒤 재요청")
                time.sleep(30)

            if exception_occurred:
                break

            if request_result:  # 해당 도로에 대한 충분한 실시간 데이터를 받아올 수 있는 경우 -> 받아온 후 머신러닝으로 예측
                predict(file_name)
                print("step : " + str(step) + ", " + file_name + " -> 예측 성공")
            else:  # 충분한 데이터가 없으면 -> 해당 도로의 2018년의 시간대별 평균 소요 시간 데이터를 사용함
                average_list = np.genfromtxt('averageElapsedTime/' + file_name + '.csv', delimiter=',')
                elapsed_time = average_list[:, 1]
                with open(result_directory_name + '/' + file_name + '.csv', 'w', newline='') as f:
                    wr = csv.writer(f)
                    for k in range(0, len(average_list)):
                        now_time = dt.now().hour
                        congestion = (int(average_list[(now_time + k - 1) % 24][1]) - sum(elapsed_time) / len(elapsed_time)) / (int(average_list[(now_time + k - 1) % 24][1]) + sum(elapsed_time) / len(elapsed_time))
                        wr.writerow([str((now_time + k - 1) % 24)] + [str(int(average_list[(now_time + k - 1) % 24][1]))] + [round(congestion, 2)])
                print("step : " + str(step) + ", " + file_name + " -> 예측 실패, 평균 소요시간 데이터 사용")
        if exception_occurred is False:
            end_time = time.time()
            now = dt.now()
            print('\n%s-%s-%s, %s:%s:%s, 모든 도로 예측에 걸린 시간 : %s초\n\n' % (now.year, now.month, now.day, now.hour, now.minute, now.second, str(round(end_time - start_time))))
            time.sleep(300)  # 5분 주기로 실시간 데이터를 받아옴


print("(실시간 영업소 간 통행 소요 시간 API) 서비스 키를 입력하세요.")
service_key = input()
main_function(service_key)
# 서비스 키 : 5584931748
