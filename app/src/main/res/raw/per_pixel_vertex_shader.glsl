uniform mat4 u_MVPMatrix;
attribute vec4 a_Color;
attribute vec4 a_Position;

varying vec4 v_Color;

void main()
{
    v_Color = a_Color;
    gl_Position = u_MVPMatrix * a_Position;
}
