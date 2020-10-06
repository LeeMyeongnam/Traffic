# 테스트용 임시 파일, 한 시퀀스당 4개의 입력 -> 한 시간 뒤의 데이터를 예측하도록 학습됨
import tensorflow as tf
import numpy as np
import matplotlib.pyplot as plt


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


# reproductivity를 위한 random seed
tf.set_random_seed(777)
folder = 'inputs\\'
fileName = '167_159'
xy = np.genfromtxt(folder + fileName + '.csv', delimiter=',')

seq_length = 6  # 학습용 데이터 시퀀스의 길이
data_dim = 7  # 학습용 데이터의 개수(월, 일, 휴일 여부, 요일 코드, 시간대, 강수량, 통행 소요 시간)
hidden_dim = 16  # hidden dimension
output_dim = 1  # 출력 데이터 개수
learning_rate = 0.01  # 학습 속도
iterations = 600  # 반복 학습 횟수

# 원래의 통행 소요 시간 데이터 -> 역정규화에 사용됨
original_elapsed_time = xy[:, 6]

# 학습에 사용될 데이터 정규화
norm_month = min_max_scaling(xy[:, 0])
norm_day = min_max_scaling(xy[:, 1])
norm_holiday = min_max_scaling(xy[:, 2])
norm_weekday = min_max_scaling(xy[:, 3])
norm_time = min_max_scaling(xy[:, 4])
norm_rain = min_max_scaling(xy[:, 5])
norm_elapsed_time = min_max_scaling(xy[:, 6])

# 학습에 사용될 정규화된 데이터를 하나의 2차원 리스트에 넣고 np array로 변환
x = []
for i in range(0, len(norm_holiday)):
    x.append((norm_month[i], norm_day[i], norm_holiday[i], norm_weekday[i], norm_time[i], norm_rain[i], norm_elapsed_time[i]))
x = np.array(x)

# 학습의 목표로 사용될 정규화된 데이터(통행 소요 시간)를 리스트에 넣고 np array로 변환
y = []
for i in range(0, len(norm_elapsed_time)):
    y.append([norm_elapsed_time[i]])
y = np.array(y)

dataX = []
dataY = []

# 각 데이터를 sequence length에 맞게 잘라내서 dataX랑 dataY에 넣음
# dataX는 월, 일, 휴일 여부, 요일 코드, 시간대, 강수량, 통행 소요 시간
# dataY는 통행 소요 시간
for i in range(0, len(y) - seq_length):
    _x = x[i:i + seq_length]
    _y = y[i + seq_length]
    # print(_x, "->", _y)
    dataX.append(_x)
    dataY.append(_y)

# 학습용 데이터 : 90%, 테스트용 데이터 : 10%
train_size = int(len(dataY) * 0.9)
test_size = len(dataY) - train_size
trainX, testX = np.array(dataX[0:train_size]), np.array(dataX[train_size:])
trainY, testY = np.array(dataY[0:train_size]), np.array(dataY[train_size:])

# input과 output의 형태 및 시퀀스와 입력 개수 정의
X = tf.placeholder(tf.float32, [None, seq_length, data_dim])
Y = tf.placeholder(tf.float32, [None, 1])

# cell 정의, 활성화 함수로 tanh 사용
cell = tf.contrib.rnn.BasicLSTMCell(num_units=hidden_dim, state_is_tuple=True, activation=tf.tanh)
# LSTM 네트워크 생성
outputs, _states = tf.nn.dynamic_rnn(cell, X, dtype=tf.float32)

# Y_pred에 train을 통해 계산된 최종 output Y가 들어가고 이 값이 testY와 비교됨
Y_pred = tf.contrib.layers.fully_connected(outputs[:, -1], output_dim, activation_fn=None)

# cost/loss 계산 -> sum of the squares
loss = tf.reduce_sum(tf.square(Y_pred - Y))

# optimizer : AdamOptimizer 사용
optimizer = tf.train.AdamOptimizer(learning_rate)
train = optimizer.minimize(loss)

# train 진행 평가척도로 RMSE 사용 -> loss를 줄이는 방향으로 학습
targets = tf.placeholder(tf.float32, [None, 1])
predictions = tf.placeholder(tf.float32, [None, 1])
rmse = tf.sqrt(tf.reduce_mean(tf.square(targets - predictions)))

# 훈련된 모델 저장에 사용되는 객체 생성
# saver = tf.train.Saver()

with tf.Session() as sess:
    init = tf.global_variables_initializer()
    sess.run(init)
    test_predict = None

    for i in range(iterations):
        _, step_loss = sess.run([train, loss], feed_dict={X: trainX, Y: trainY})
        test_predict = sess.run(Y_pred, feed_dict={X: testX})
        rmse_val = sess.run(rmse, feed_dict={targets: testY, predictions: test_predict})
        print("[step: {}] loss: {}, RMSE: {}".format(i, step_loss, rmse_val))
    print("min: ", np.min(original_elapsed_time), "max: ", np.max(original_elapsed_time), "average: ", np.mean(original_elapsed_time))
    # saver.save(sess, 'models_test/' + fileName + '.ckpt')

    # 학습 결과 그래프 생성
    plt.plot(reverse_min_max_scaling(original_elapsed_time, testY), "b")
    plt.plot(reverse_min_max_scaling(original_elapsed_time, test_predict), "r")
    plt.xlabel("Time Period")
    plt.ylabel("Elapsed Time")
    plt.show()


"""
    # 원래의 값과 예측 값 출력
    for j in range(0, len(testY)):
        print("원래 값 : ", reverse_min_max_scaling(original_elapsed_time, testY[j]), "\t예측 값 : ", reverse_min_max_scaling(original_elapsed_time, test_predict[j]))
"""
